package io.netty.channel.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopException;
import io.netty.channel.SelectStrategy;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.IntSupplier;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReflectionUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link SingleThreadEventLoop} implementation which register the {@link Channel}'s to a
 * {@link Selector} and so does the multi-plexing of these in the event loop.
 *
 */
public final class NioEventLoop extends SingleThreadEventLoop {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioEventLoop.class);

    private static final int CLEANUP_INTERVAL = 256; // XXX Hard-coded value, but won't need customization.

    private static final boolean DISABLE_KEYSET_OPTIMIZATION =
            SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);

    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;

    private final IntSupplier selectNowSupplier = new IntSupplier() {
        @Override
        public int get() throws Exception {
            return selectNow();
        }
    };
    private final Callable<Integer> pendingTasksCallable = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            return NioEventLoop.super.pendingTasks();
        }
    };

    // Workaround for JDK NIO bug.
    //
    // See:
    // - http://bugs.sun.com/view_bug.do?bug_id=6427854
    // - https://github.com/netty/netty/issues/203
    static {
        final String key = "sun.nio.ch.bugLevel";
        final String buglevel = SystemPropertyUtil.get(key);
        if (buglevel == null) {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        System.setProperty(key, "");
                        return null;
                    }
                });
            } catch (final SecurityException e) {
                logger.debug("Unable to get/set System Property: " + key, e);
            }
        }

        int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
        if (selectorAutoRebuildThreshold < MIN_PREMATURE_SELECTOR_RETURNS) {
            selectorAutoRebuildThreshold = 0;
        }

        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.noKeySetOptimization: {}", DISABLE_KEYSET_OPTIMIZATION);
            logger.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", SELECTOR_AUTO_REBUILD_THRESHOLD);
        }
    }

    /**
     * The NIO {@link Selector}.
     * 每个nioeventloop 都有一个selector
     */
    private Selector selector;
    private Selector unwrappedSelector;
    //优化的
    private SelectedSelectionKeySet selectedKeys;

    private final SelectorProvider provider;

    /**
     * Boolean that controls determines if a blocked Selector.select should
     * break out of its selection process. In our case we use a timeout for
     * the select method and the select method will block for that time unless
     * waken up.
     */
    private final AtomicBoolean wakenUp = new AtomicBoolean();

    private final SelectStrategy selectStrategy;

    private volatile int ioRatio = 50;
    private int cancelledKeys;
    private boolean needsToSelectAgain;

    //NioEventLoop这个类的构造方法
    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, false, DEFAULT_MAX_PENDING_TASKS, rejectedExecutionHandler);

        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }
        provider = selectorProvider;
        final SelectorTuple selectorTuple = openSelector();
        System.out.println("===========选择器的绑定============");

        selector = selectorTuple.selector;
        unwrappedSelector = selectorTuple.unwrappedSelector;
        selectStrategy = strategy;
    }

    private static final class SelectorTuple {
        final Selector unwrappedSelector;
        final Selector selector;

        SelectorTuple(Selector unwrappedSelector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = unwrappedSelector;
        }

        SelectorTuple(Selector unwrappedSelector, Selector selector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = selector;
        }
    }

    private SelectorTuple openSelector() {
        final Selector unwrappedSelector;
        try {
            System.out.println("==========使用原生的JDK产生一个selector==========");
            unwrappedSelector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }

        //不需要优化，直接返回
        if (DISABLE_KEYSET_OPTIMIZATION) {
            return new SelectorTuple(unwrappedSelector);
        }

        //披着羊皮的狼，用数组替换 原生JDK的hashSet
        final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();

        Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    return Class.forName(
                            "sun.nio.ch.SelectorImpl",
                            false,
                            PlatformDependent.getSystemClassLoader());
                } catch (Throwable cause) {
                    return cause;
                }
            }
        });

        if (!(maybeSelectorImplClass instanceof Class) ||
                // ensure the current selector implementation is what we can instrument.
                !((Class<?>) maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass())) {
            if (maybeSelectorImplClass instanceof Throwable) {
                Throwable t = (Throwable) maybeSelectorImplClass;
                logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, t);
            }
            return new SelectorTuple(unwrappedSelector);
        }

        final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;

        Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
                    Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");

                    Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField);
                    if (cause != null) {
                        return cause;
                    }
                    cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField);
                    if (cause != null) {
                        return cause;
                    }

                    selectedKeysField.set(unwrappedSelector, selectedKeySet);
                    publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
                    return null;
                } catch (NoSuchFieldException e) {
                    return e;
                } catch (IllegalAccessException e) {
                    return e;
                }
            }
        });

        if (maybeException instanceof Exception) {
            selectedKeys = null;
            Exception e = (Exception) maybeException;
            logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, e);
            return new SelectorTuple(unwrappedSelector);
        }
        selectedKeys = selectedKeySet;
        logger.trace("instrumented a special java.util.Set into: {}", unwrappedSelector);
        return new SelectorTuple(unwrappedSelector,
                                 new SelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
    }

    /**
     * Returns the {@link SelectorProvider} used by this {@link NioEventLoop} to obtain the {@link Selector}.
     */
    public SelectorProvider selectorProvider() {
        return provider;
    }

    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        System.out.println("========创建队列==============");
        // This event loop never calls takeTask()
        //多生产者单消费者队列，netty使用mpsc，方便的将外部线程的task聚集，
        // 在reactor线程内部用单线程来串行执行，我们可以借鉴netty的任务执行模式来处理类似多线程数据上报，定时聚合的应用
        return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.<Runnable>newMpscQueue()
                                                    : PlatformDependent.<Runnable>newMpscQueue(maxPendingTasks);
    }

    @Override
    public int pendingTasks() {
        // As we use a MpscQueue we need to ensure pendingTasks() is only executed from within the EventLoop as
        // otherwise we may see unexpected behavior (as size() is only allowed to be called by a nio consumer).
        // See https://github.com/netty/netty/issues/5297
        if (inEventLoop()) {
            return super.pendingTasks();
        } else {
            return submit(pendingTasksCallable).syncUninterruptibly().getNow();
        }
    }

    /**
     * Registers an arbitrary {@link SelectableChannel}, not necessarily created by Netty, to the {@link Selector}
     * of this event loop.  Once the specified {@link SelectableChannel} is registered, the specified {@code task} will
     * be executed by this event loop when the {@link SelectableChannel} is ready.
     */
    public void register(final SelectableChannel ch, final int interestOps, final NioTask<?> task) {
        if (ch == null) {
            throw new NullPointerException("ch");
        }
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ch.validOps()) != 0) {
            throw new IllegalArgumentException(
                    "invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
        if (task == null) {
            throw new NullPointerException("task");
        }

        if (isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }

        try {
            ch.register(selector, interestOps, task);
        } catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    /**
     * Returns the percentage of the desired amount of time spent for I/O in the event loop.
     */
    public int getIoRatio() {
        return ioRatio;
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the event loop.  The default value is
     * {@code 50}, which means the event loop will try to spend the same amount of time for I/O as for non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }

    /**
     * Replaces the current {@link Selector} of this event loop with newly created {@link Selector}s to work
     * around the infamous epoll 100% CPU bug.
     */
    public void rebuildSelector() {
        if (!inEventLoop()) {
            execute(new Runnable() {
                @Override
                public void run() {
                    rebuildSelector0();
                }
            });
            return;
        }
        rebuildSelector0();
    }

    /**
     *拿到有效的key
     * 取消该key在旧的selector上的事件注册
     * 将该key对应的channel注册到新的selector上
     * 重新绑定channel和新的key的关系
     */
    private void rebuildSelector0() {
        final Selector oldSelector = selector;
        final SelectorTuple newSelectorTuple;

        if (oldSelector == null) {
            return;
        }

        try {
            //通过openSelector()方法创建一个新的selector，然后执行一个死循环，只要执行过程中出现过一次并发修改selectionKeys异常，就重新开始转移
            newSelectorTuple = openSelector();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        // Register all channels to the new Selector.
        int nChannels = 0;
        for (SelectionKey key: oldSelector.keys()) {
            Object a = key.attachment();
            try {
                if (!key.isValid() || key.channel().keyFor(newSelectorTuple.unwrappedSelector) != null) {
                    continue;
                }

                int interestOps = key.interestOps();
                key.cancel();
                SelectionKey newKey = key.channel().register(newSelectorTuple.unwrappedSelector, interestOps, a);
                if (a instanceof AbstractNioChannel) {
                    // Update SelectionKey
                    ((AbstractNioChannel) a).selectionKey = newKey;
                }
                nChannels ++;
            } catch (Exception e) {
                logger.warn("Failed to re-register a Channel to the new Selector.", e);
                if (a instanceof AbstractNioChannel) {
                    AbstractNioChannel ch = (AbstractNioChannel) a;
                    ch.unsafe().close(ch.unsafe().voidPromise());
                } else {
                    @SuppressWarnings("unchecked")
                    NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                    invokeChannelUnregistered(task, key, e);
                }
            }
        }

        selector = newSelectorTuple.selector;
        unwrappedSelector = newSelectorTuple.unwrappedSelector;

        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }

        logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
    }

    /**
     * nioEventloop（简称loop），其内部持有一个thread对象，而该loop的run方法正是由该线程执行的也就是reactor的执行体
     */
    /**
     * todo select()  检查是否有IO事件  轮询IO事件
     * todo ProcessorSelectedKeys()    处理IO事件
     * todo RunAllTask()    处理异步任务队列
     */
    @Override
    protected void run() {
        //NioEventLoop循环执行是不是从中取任务然后执行
        for (;;) {
            try {
                //// 当有任务时为了保证任务及时执行采用不阻塞的selectNow获取准备好I/O的连接
                //当无任务时采用阻塞等待的方式获取连接
                switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
                    case SelectStrategy.CONTINUE:
                        continue;
                    case SelectStrategy.SELECT:

                        //wakenUp 表示是否应该唤醒正在阻塞的select操作，可以看到netty在进行一次新的loop之前，都会将wakeUp 被设置成false
                        boolean andSet = wakenUp.getAndSet(false);
                        //首先轮询注册到reactor线程对用的selector上的所有的channel的IO事件
                        select(andSet);
                        // 'wakenUp.compareAndSet(false, true)' is always evaluated
                        // before calling 'selector.wakeup()' to reduce the wake-up
                        // overhead. (Selector.wakeup() is an expensive operation.)
                        //
                        // However, there is a race condition in this approach.
                        // The race condition is triggered when 'wakenUp' is set to
                        // true too early.
                        //
                        // 'wakenUp' is set to true too early if:
                        // 1) Selector is waken up between 'wakenUp.set(false)' and
                        //    'selector.select(...)'. (BAD)
                        // 2) Selector is waken up between 'selector.select(...)' and
                        //    'if (wakenUp.get()) { ... }'. (OK)
                        //
                        // In the first case, 'wakenUp' is set to true and the
                        // following 'selector.select(...)' will wake up immediately.
                        // Until 'wakenUp' is set to false again in the next round,
                        // 'wakenUp.compareAndSet(false, true)' will fail, and therefore
                        // any attempt to wake up the Selector will fail, too, causing
                        // the following 'selector.select(...)' call to block
                        // unnecessarily.
                        //
                        // To fix this problem, we wake up the selector again if wakenUp
                        // is true immediately after selector.select(...).
                        // It is inefficient in that it wakes up the selector for both
                        // the first case (BAD - wake-up required) and the second case
                        // (OK - no wake-up required).

                        //这里不太明白 上面不是设置为false 这里肯定进不来啊？？除非select方法里面可以设置
                        if (wakenUp.get()) {
                            selector.wakeup();
                        }
                        // fall through
                    default:
                }

                cancelledKeys = 0;
                needsToSelectAgain = false;
                final int ioRatio = this.ioRatio;  //默认是50
                if (ioRatio == 100) {
                    try {
                        //处理产生网络IO事件的channel
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        //处理任务队列
                        runAllTasks();
                    }
                } else {
                    final long ioStartTime = System.nanoTime();
                    try {
                        //处理I/O事件，
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        final long ioTime = System.nanoTime() - ioStartTime;

                        //外部线程 扔到这个线程里面的任务 (todo 用于处理 本 eventLoop外的线程 扔到taskQueue中的任务)
                        //注意这里有聚合任务的逻辑
                        runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
            // Always handle shutdown even if the loop processing threw an exception.
            try {
                if (isShuttingDown()) {
                    closeAll();
                    if (confirmShutdown()) {
                        return;
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
        }
    }

    private static void handleLoopException(Throwable t) {
        logger.warn("Unexpected exception in the selector loop.", t);

        // Prevent possible consecutive immediate failures that lead to
        // excessive CPU consumption.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }

    /**
     * 当有了新IO请求进来, jdk原生的Selector将SelectionKey放入存放感兴趣的key的集合中,
     * 而这个集合现在就是netty通过反射的方式强制替换为以数组为数据结构的selectedKeys, 数组不为空, 跟进processSelectedKeysOptimized()
     */
    private void processSelectedKeys() {
        //优化过的selector 中的selectedKeys 不为空
        //  selectedKeys 就是经过优化后的keys(底层是数组)
        if (selectedKeys != null) {
            processSelectedKeysOptimized();
        } else {
            processSelectedKeysPlain(selector.selectedKeys());
        }
    }

    @Override
    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            logger.warn("Failed to close a selector.", e);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        cancelledKeys ++;
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            needsToSelectAgain = true;
        }
    }

    @Override
    protected Runnable pollTask() {
        Runnable task = super.pollTask();
        if (needsToSelectAgain) {
            selectAgain();
        }
        return task;
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        System.out.println("=======处理IO=2=============");
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }

        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();

            if (a instanceof AbstractNioChannel) {
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (!i.hasNext()) {
                break;
            }

            if (needsToSelectAgain) {
                selectAgain();
                selectedKeys = selector.selectedKeys();

                // Create the iterator again to avoid ConcurrentModificationException
                if (selectedKeys.isEmpty()) {
                    break;
                } else {
                    i = selectedKeys.iterator();
                }
            }
        }
    }

    /**
     * 对IO事件进行处理
     * 优化过的
     */
    private void processSelectedKeysOptimized() {

        for (int i = 0; i < selectedKeys.size; ++i) {

            // 1.取出IO事件以及对应的channel
            //优化过的 SelectedSelectionKeySet 的好处，遍历的时候遍历的是数组，相对jdk原生的HashSet效率有所提高
            final SelectionKey k = selectedKeys.keys[i];


           // 拿到当前SelectionKey之后，将selectedKeys[i]置为null，这里简单解释一下这么做的理由：
            // 想象一下这种场景，假设一个NioEventLoop平均每次轮询出N个IO事件，高峰期轮询出3N个事件，那么selectedKeys的物理长度要大于等于3N，
            // 如果每次处理这些key，不置selectedKeys[i]为空，那么高峰期一过，这些保存在数组尾部的selectedKeys[i]对应的SelectionKey将
            // 一直无法被回收，SelectionKey对应的对象可能不大，但是要知道，它可是有attachment的，这里的attachment具体是什么下面会讲到，
            // 但是有一点我们必须清楚，attachment可能很大，这样一来，这些元素是GC root可达的，很容易造成gc不掉，内存泄漏就发生了


            //  数组输出空项, 从而允许在channel 关闭时对其进行垃圾回收
            // null out entry in the array to allow to have it GC'ed once the Channel close
            // See https://github.com/netty/netty/issues/2363
            //  数组中当前循环对应的keys质空, 这种感兴趣的事件只处理一次就行
            selectedKeys.keys[i] = null;


            //  获取出 attachment,默认情况下就是注册进Selector时,传入的第三个参数  this===>   NioServerSocketChannel
            //  一个Selector中可能被绑定上了成千上万个Channel,  通过K+attachment 的手段,
            //  精确的取出发生指定事件的channel, 进而获取channel中的unsafe类进行下一步处理
            final Object a = k.attachment();

            // 2.处理该channel
            //  这里为什么是AbstractNioChannel 类型
            // 查看AbstractNioChannel#doRegister()方法
            /*
            * 我们查看到SelectableChannel方法，结合netty的 doRegister() 方法，
            * 我们不难推论出，netty的轮询注册机制其实是将AbstractNioChannel内部的jdk类SelectableChannel对象注册到jdk类Selctor对象上去，
            * 并且将AbstractNioChannel作为SelectableChannel对象的一个attachment附属上，这样再jdk轮询出某条SelectableChannel有IO事件发生时，
            * 就可以直接取出AbstractNioChannel进行后续操作
             */
            if (a instanceof AbstractNioChannel) {
                //  进入这个方法, 传进入 感兴趣的key + NioSocketChannel
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }
            // 3.判断是否该再来次轮询
            if (needsToSelectAgain) {
                // null out entries in the array to allow to have it GC'ed once the Channel close
                // See https://github.com/netty/netty/issues/2363
                selectedKeys.reset(i + 1);

                selectAgain();
                i = -1;
            }
        }
    }

    /**
     * 处理IO事件 其实IO事件就两个
     *1.对于boss NioEventLoop来说，轮询到的是基本上就是连接事件，后续的事情就通过他的pipeline将连接扔给一个worker NioEventLoop处理
     * 2.对于worker NioEventLoop来说，轮询到的基本上都是io读写事件，后续的事情就是通过他的pipeline将读取到的字节流传递给每个channelHandler来处理
     * @param k
     * @param ch
     */
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {

        //  这个unsafe 也是可channel 也是和Channel进行唯一绑定的对象
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        //首先检查获取的key 是不是合法的
        if (!k.isValid()) {

            //非法的直接退出
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                // If the channel implementation throws an exception because there is no event loop, we ignore this
                // because we are only trying to determine if ch is registered to this event loop and thus has authority
                // to close ch.
                return;
            }
            // Only close ch if ch is still registered to this EventLoop. ch could have deregistered from the event loop
            // and thus the SelectionKey could be cancelled as part of the deregistration process, but the channel is
            // still healthy and should not be closed.
            // See https://github.com/netty/netty/issues/5125
            //  确保多线程下的安全性
            if (eventLoop != this || eventLoop == null) {
                return;
            }
            // close the channel if the key is not valid anymore
            unsafe.close(unsafe.voidPromise());
            return;
        }
        //  NioServerSocketChannel和selectKey都合法的话, 就进入下面的 处理阶段
        try {
            //  获取SelectedKey 的 关心的选项
            int readyOps = k.readyOps();
            // We first need to call finishConnect() before try to trigger a read(...) or write(...) as otherwise
            // the NIO JDK channel implementation may throw a NotYetConnectedException.
            //  在read()   write()之前我们需要调用 finishConnect()  方法, 否则  NIO JDK抛出异常
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
                // See https://github.com/netty/netty/issues/924
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);

                unsafe.finishConnect();
            }

            // Process OP_WRITE first as we may be able to write some queued buffers and so free memory.
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
                ch.unsafe().forceFlush();
            }

            // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
            // to a spin loop
            //处理连接事件   readyOps=16  SelectionKey.OP_ACCEPT
            //  同样是检查 readOps是否为零, 来检查是否出现了  jdk  空轮询的bug
            System.out.println((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0);
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                //NioMessageUnsafe
                System.out.println("=====readyOpsreadyOps==========="+readyOps);
                System.out.println("=====这里是处理连接还是读写========"+unsafe);

                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }

    private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
        int state = 0;
        try {
            task.channelReady(k.channel(), k);
            state = 1;
        } catch (Exception e) {
            k.cancel();
            invokeChannelUnregistered(task, k, e);
            state = 2;
        } finally {
            switch (state) {
            case 0:
                k.cancel();
                invokeChannelUnregistered(task, k, null);
                break;
            case 1:
                if (!k.isValid()) { // Cancelled by channelReady()
                    invokeChannelUnregistered(task, k, null);
                }
                break;
            }
        }
    }

    private void closeAll() {
        selectAgain();
        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractNioChannel> channels = new ArrayList<AbstractNioChannel>(keys.size());
        for (SelectionKey k: keys) {
            Object a = k.attachment();
            if (a instanceof AbstractNioChannel) {
                channels.add((AbstractNioChannel) a);
            } else {
                k.cancel();
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                invokeChannelUnregistered(task, k, null);
            }
        }

        for (AbstractNioChannel ch: channels) {
            ch.unsafe().close(ch.unsafe().voidPromise());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k, Throwable cause) {
        try {
            task.channelUnregistered(k.channel(), cause);
        } catch (Exception e) {
            logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && wakenUp.compareAndSet(false, true)) {
            selector.wakeup();
        }
    }

    Selector unwrappedSelector() {
        return unwrappedSelector;
    }

    int selectNow() throws IOException {
        try {
            return selector.selectNow();
        } finally {
            // restore wakeup state if needed
            if (wakenUp.get()) {
                selector.wakeup();
            }
        }
    }

    /**
     * 三个工程
     * @param oldWakenUp
     * @throws IOException
     */
    private void select(boolean oldWakenUp) throws IOException {
        //每次操作之前都标记为未唤醒状态？？
        Selector selector = this.selector;
        try {
            int selectCnt = 0;
            //当前时间
            long currentTimeNanos = System.nanoTime();
            //获取的是什么时间？
            long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);
            //也是个循环 什么时候退出？
            for (;;) {
                // 1.定时任务截止事时间快到了，中断本次轮询
                long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;
                //超时的话，直接执行一个非阻塞的select操作，然后结束本次循环
                if (timeoutMillis <= 0) {
                    if (selectCnt == 0) {
                        selector.selectNow();
                        selectCnt = 1;
                    }
                    break;
                }


                // 2.轮询过程中发现有任务加入，中断本次轮询
                // If a task was submitted when wakenUp value was true, the task didn't get a chance to call
                // Selector#wakeup. So we need to check task queue again before executing select operation.
                // If we don't, the task might be pended until select operation was timed out.
                // It might be pended until idle timeout if IdleStateHandler existed in pipeline.
                //同样的 加入队列里面有任务和上次一样  设置wakeup 原因是，外部线程 可以设置？
                if (hasTasks() && wakenUp.compareAndSet(false, true)) {
                    //非阻塞式,可以立即返回
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }


                // 3.阻塞式select操作
                //当前队列为空，并且时间没到 下面执行阻塞式select 操作
                int selectedKeys = selector.select(timeoutMillis);
                selectCnt ++;
               // 轮询到IO事件 （selectedKeys != 0）
                // oldWakenUp 参数为true
                // 任务队列里面有任务（hasTasks）
                // 第一个定时任务即将要被执行 （hasScheduledTasks（））
                // 用户主动唤醒（wakenUp.get()）

                if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || hasTasks() || hasScheduledTasks()) {
                    // - Selected something,
                    // - waken up by user, or
                    // - the task queue has a pending task.
                    // - a scheduled task is ready for processing
                    break;
                }


                if (Thread.interrupted()) {
                    // Thread was interrupted so reset selected keys and break so we not run into a busy loop.
                    // As this is most likely a bug in the handler of the user or it's client library we will
                    // also log it.
                    //
                    // See https://github.com/netty/netty/issues/2426
                    if (logger.isDebugEnabled()) {
                        logger.debug("Selector.select() returned prematurely because " +
                                "Thread.currentThread().interrupt() was called. Use " +
                                "NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
                    }
                    selectCnt = 1;
                    break;
                }

                /*空轮训bug解决*/
                //netty 会在每次进行 selector.select(timeoutMillis) 之前记录一下开始时间currentTimeNanos，在select之后记录一下结束时间，
                // 判断select操作是否至少持续了timeoutMillis秒
                // （这里将time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos
                // 改成time - currentTimeNanos >= TimeUnit.MILLISECONDS.toNanos(timeoutMillis)或许更好理解一些）

                // 执行至此,说明已进行了一次阻塞式的 select 操作
                long time = System.nanoTime();
                if (time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos) {
                    // timeoutMillis elapsed without anything selected.
                    selectCnt = 1;
                    //下面分支说明出现空轮训
                } else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 && selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD)
                {   //空轮训的次数大于给定的值
                    // The selector returned prematurely many times in a row.
                    // Rebuild the selector to work around the problem.
                    logger.warn(
                            "Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                            selectCnt, selector);

                    //创建新的selector  把旧的重新注册
                    //于是nettyrebuild()Selector ,从新开启一个Selector, 循环老的Selector上面的上面的注册的事件,
                    // 重新注册进新的 Selector上,用这个中替换Selector的方法,解决了空轮询的bug
                    rebuildSelector();
                    selector = this.selector;

                    // Select again to populate selectedKeys.
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }

                currentTimeNanos = time;
            }

            if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Selector.select() returned prematurely {} times in a row for Selector {}.",
                            selectCnt - 1, selector);
                }
            }
        } catch (CancelledKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                        selector, e);
            }
            // Harmless exception - log anyway
        }
    }

    private void selectAgain() {
        needsToSelectAgain = false;
        try {
            selector.selectNow();
        } catch (Throwable t) {
            logger.warn("Failed to update SelectionKeys.", t);
        }
    }
}
