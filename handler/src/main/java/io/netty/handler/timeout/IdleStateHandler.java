package io.netty.handler.timeout;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Triggers an {@link IdleStateEvent} when a {@link Channel} has not performed
 * read, write, or both operation for a while.
 *
 * <h3>Supported idle states</h3>
 * <table border="1">
 * <tr>
 * <th>Property</th><th>Meaning</th>
 * </tr>
 * <tr>
 * <td>{@code readerIdleTime}</td>
 * <td>an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
 *     will be triggered when no read was performed for the specified period of
 *     time.  Specify {@code 0} to disable.</td>
 * </tr>
 * <tr>
 * <td>{@code writerIdleTime}</td>
 * <td>an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
 *     will be triggered when no write was performed for the specified period of
 *     time.  Specify {@code 0} to disable.</td>
 * </tr>
 * <tr>
 * <td>{@code allIdleTime}</td>
 * <td>an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
 *     will be triggered when neither read nor write was performed for the
 *     specified period of time.  Specify {@code 0} to disable.</td>
 * </tr>
 * </table>
 *
 * <pre>
 * // An example that sends a ping message when there is no outbound traffic
 * // for 30 seconds.  The connection is closed when there is no inbound traffic
 * // for 60 seconds.
 *
 * public class MyChannelInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("idleStateHandler", new {@link IdleStateHandler}(60, 30, 0));
 *         channel.pipeline().addLast("myHandler", new MyHandler());
 *     }
 * }
 *
 * // Handler should handle the {@link IdleStateEvent} triggered by {@link IdleStateHandler}.
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *     {@code @Override}
 *     public void userEventTriggered({@link ChannelHandlerContext} ctx, {@link Object} evt) throws {@link Exception} {
 *         if (evt instanceof {@link IdleStateEvent}) {
 *             {@link IdleStateEvent} e = ({@link IdleStateEvent}) evt;
 *             if (e.state() == {@link IdleState}.READER_IDLE) {
 *                 ctx.close();
 *             } else if (e.state() == {@link IdleState}.WRITER_IDLE) {
 *                 ctx.writeAndFlush(new PingMessage());
 *             }
 *         }
 *     }
 * }
 *
 * {@link ServerBootstrap} bootstrap = ...;
 * ...
 * bootstrap.childHandler(new MyChannelInitializer());
 * ...
 * </pre>
 *
 * @see ReadTimeoutHandler
 * @see WriteTimeoutHandler
 * todo 当 Channel 的读或者写空闲时间太长时，将会触发一个 IdleStateEvent 事件。
 *  然后，你可以自定义一个 ChannelInboundHandler ，重写 #userEventTriggered(ChannelHandlerContext ctx, Object evt) 方法，处理该事件
 */
public class IdleStateHandler extends ChannelDuplexHandler {

    /**
     * 最小的超时时间，单位：纳秒
     */
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    /**
     * 写入任务监听器
     * 写入操作，完成 flush 到对端的回调监听器。初始时，创建好，避免重复创建，从而减轻 GC 压力
     */
    // Not create a new ChannelFutureListener per write operation to reduce GC pressure.
    private final ChannelFutureListener writeListener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            // 记录最后写时间
            lastWriteTime = ticksInNanos();
            // 重置 firstWriterIdleEvent 和 firstAllIdleEvent 为 true
            firstWriterIdleEvent = firstAllIdleEvent = true;
        }
    };
    /**
     * 是否观察 {@link ChannelOutboundBuffer} 写入队列
     */
    private final boolean observeOutput;
    /**
     * 配置的读空闲时间，单位：纳秒
     */
    private final long readerIdleTimeNanos;
    /**
     * 配置的写空闲时间，单位：纳秒
     */
    private final long writerIdleTimeNanos;
    /**
     * 配置的All( 读或写任一 )，单位：纳秒
     */
    private final long allIdleTimeNanos;
    /**
     * 读空闲的定时检测任务
     */
    private ScheduledFuture<?> readerIdleTimeout;
    /**
     * 最后读时间
     */
    private long lastReadTime;
    /**
     * 是否首次读空闲
     */
    private boolean firstReaderIdleEvent = true;
    /**
     * 写空闲的定时检测任务
     */

    private ScheduledFuture<?> writerIdleTimeout;
    /**
     * 最后写时间
     */
    private long lastWriteTime;
    /**
     * 是否首次写空闲
     */
    private boolean firstWriterIdleEvent = true;
    /**
     * All 空闲时间，单位：纳秒
     */
    private ScheduledFuture<?> allIdleTimeout;
    /**
     * 是否首次 All 空闲
     */
    private boolean firstAllIdleEvent = true;
    /**
     * 状态
     *
     * 0 - none ，未初始化
     * 1 - initialized ，已经初始化
     * 2 - destroyed ，已经销毁
     */
    private byte state; // 0 - none, 1 - initialized, 2 - destroyed


    /**
     * 是否正在读取
     */
    private boolean reading;
    /**
     * 最后检测到 {@link ChannelOutboundBuffer} 发生变化的时间
     */
    private long lastChangeCheckTimeStamp;
    /**
     * 第一条准备 flash 到对端的消息( {@link ChannelOutboundBuffer#current()} )的 HashCode
     */
    private int lastMessageHashCode;
    /**
     * 总共等待 flush 到对端的内存大小( {@link ChannelOutboundBuffer#totalPendingWriteBytes()} )
     */
    private long lastPendingWriteBytes;

    /**
     * Creates a new instance firing {@link IdleStateEvent}s.
     *
     * @param readerIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     *        will be triggered when no read was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param writerIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     *        will be triggered when no write was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param allIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
     *        will be triggered when neither read nor write was performed for
     *        the specified period of time.  Specify {@code 0} to disable.
     */
    public IdleStateHandler(
            int readerIdleTimeSeconds,
            int writerIdleTimeSeconds,
            int allIdleTimeSeconds) {

        this(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds,
             TimeUnit.SECONDS);
    }

    /**
     * @see #IdleStateHandler(boolean, long, long, long, TimeUnit)
     */
    public IdleStateHandler(
            long readerIdleTime, long writerIdleTime, long allIdleTime,
            TimeUnit unit) {
        this(false, readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    /**
     * Creates a new instance firing {@link IdleStateEvent}s.
     *
     * @param observeOutput
     *        whether or not the consumption of {@code bytes} should be taken into
     *        consideration when assessing write idleness. The default is {@code false}.
     * @param readerIdleTime
     *        an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     *        will be triggered when no read was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param writerIdleTime
     *        an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     *        will be triggered when no write was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param allIdleTime
     *        an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
     *        will be triggered when neither read nor write was performed for
     *        the specified period of time.  Specify {@code 0} to disable.
     * @param unit
     *        the {@link TimeUnit} of {@code readerIdleTime},
     *        {@code writeIdleTime}, and {@code allIdleTime}
     */
    public IdleStateHandler(boolean observeOutput,
            long readerIdleTime, long writerIdleTime, long allIdleTime,
            TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        this.observeOutput = observeOutput;

        if (readerIdleTime <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = Math.max(unit.toNanos(readerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (writerIdleTime <= 0) {
            writerIdleTimeNanos = 0;
        } else {
            writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (allIdleTime <= 0) {
            allIdleTimeNanos = 0;
        } else {
            allIdleTimeNanos = Math.max(unit.toNanos(allIdleTime), MIN_TIMEOUT_NANOS);
        }
    }

    /**
     * Return the readerIdleTime that was given when instance this class in milliseconds.
     *
     */
    public long getReaderIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(readerIdleTimeNanos);
    }

    /**
     * Return the writerIdleTime that was given when instance this class in milliseconds.
     *
     */
    public long getWriterIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(writerIdleTimeNanos);
    }

    /**
     * Return the allIdleTime that was given when instance this class in milliseconds.
     *
     */
    public long getAllIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(allIdleTimeNanos);
    }

    /**
     * 当 Channel 被激活后，动态添加此 Handler ，则 handlerAdded 方法的初始化被调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            // 初始化
            initialize(ctx);
        } else {
            // channelActive() event has not been fired yet.  this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // 初始化
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            initialize(ctx);
        }
        // 继续传播 Channel Registered 事件到下一个节点
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        // 初始化
        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 开启了 read 或 all 的空闲检测
        if (readerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            // 标记正在读取
            reading = true;
            // 重置 firstWriterIdleEvent 和 firstAllIdleEvent 为 true 即又变成首次。
            firstReaderIdleEvent = firstAllIdleEvent = true;
        }
        // 继续传播 Channel Read 事件到下一个节点
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 开启了 read 或 all 的空闲检测
        if ((readerIdleTimeNanos > 0 || allIdleTimeNanos > 0) && reading) {
            // 记录最后读时间
            lastReadTime = ticksInNanos();
            // 标记不在读取
            reading = false;
        }
        // 继续传播 Channel ReadComplete 事件到下一个节点
        ctx.fireChannelReadComplete();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // 开启了 write 或 all 的空闲检测
        // Allow writing with void promise if handler is only configured for read timeout events.
        if (writerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            // 写入，并添加写入监听器
            //该监听器会在消息( 数据 ) flush 到对端后，回调，修改最后写入时间 lastWriteTime 为 #ticksInNanos()
            ctx.write(msg, promise.unvoid()).addListener(writeListener);
        } else {
            // 写入，不添加监听器
            ctx.write(msg, promise);
        }
    }

    private void initialize(ChannelHandlerContext ctx) {
        // 校验状态，避免因为 `#destroy()` 方法在 `#initialize(ChannelHandlerContext ctx)` 方法，执行之前
        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        switch (state) {
        case 1:
        case 2:
            return;
        }
        // 标记为已初始化
        state = 1;
        // 初始化 ChannelOutboundBuffer 相关属性
        initOutputChanged(ctx);
        // 初始相应的定时任务
        lastReadTime = lastWriteTime = ticksInNanos();
        if (readerIdleTimeNanos > 0) {
            readerIdleTimeout = schedule(ctx, new ReaderIdleTimeoutTask(ctx),
                    readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
        if (writerIdleTimeNanos > 0) {
            writerIdleTimeout = schedule(ctx, new WriterIdleTimeoutTask(ctx),
                    writerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
        if (allIdleTimeNanos > 0) {
            allIdleTimeout = schedule(ctx, new AllIdleTimeoutTask(ctx),
                    allIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * This method is visible for testing!
     */
    long ticksInNanos() {
        return System.nanoTime();
    }

    /**
     * This method is visible for testing!
     */
    ScheduledFuture<?> schedule(ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit) {
        return ctx.executor().schedule(task, delay, unit);
    }

    private void destroy() {
        // 标记为销毁
        state = 2;
        // 销毁相应的定时任务
        if (readerIdleTimeout != null) {
            readerIdleTimeout.cancel(false);
            readerIdleTimeout = null;
        }
        if (writerIdleTimeout != null) {
            writerIdleTimeout.cancel(false);
            writerIdleTimeout = null;
        }
        if (allIdleTimeout != null) {
            allIdleTimeout.cancel(false);
            allIdleTimeout = null;
        }
    }

    /**
     *   todo  在 pipeline 中，触发 UserEvent 事件
     * Is called when an {@link IdleStateEvent} should be fired. This implementation calls
     * {@link ChannelHandlerContext#fireUserEventTriggered(Object)}.
     */
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    /**
     * 在定时任务中，如果检测到空闲
     * Returns a {@link IdleStateEvent}.
     */
    protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
        switch (state) {
            case ALL_IDLE:
                return first ? IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT : IdleStateEvent.ALL_IDLE_STATE_EVENT;
            case READER_IDLE:
                return first ? IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT : IdleStateEvent.READER_IDLE_STATE_EVENT;
            case WRITER_IDLE:
                return first ? IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT : IdleStateEvent.WRITER_IDLE_STATE_EVENT;
            default:
                throw new IllegalArgumentException("Unhandled: state=" + state + ", first=" + first);
        }
    }

    /**
     * @see #hasOutputChanged(ChannelHandlerContext, boolean)
     */
    private void initOutputChanged(ChannelHandlerContext ctx) {
        if (observeOutput) {
            Channel channel = ctx.channel();
            Unsafe unsafe = channel.unsafe();
            ChannelOutboundBuffer buf = unsafe.outboundBuffer();

            if (buf != null) {
                // 记录第一条准备 flash 到对端的消息的 HashCode
                lastMessageHashCode = System.identityHashCode(buf.current());
                // 记录总共等待 flush 到对端的内存大小
                lastPendingWriteBytes = buf.totalPendingWriteBytes();
            }
        }
    }

    /**
     * Returns {@code true} if and only if the {@link IdleStateHandler} was constructed
     * with {@link #observeOutput} enabled and there has been an observed change in the
     * {@link ChannelOutboundBuffer} between two consecutive calls of this method.
     *
     * https://github.com/netty/netty/issues/6150
     */
    private boolean hasOutputChanged(ChannelHandlerContext ctx, boolean first) {
        // 开启观察 ChannelOutboundBuffer 队列
        if (observeOutput) {

            // We can take this shortcut if the ChannelPromises that got passed into write()
            // appear to complete. It indicates "change" on message level and we simply assume
            // that there's change happening on byte level. If the user doesn't observe channel
            // writability events then they'll eventually OOME and there's clearly a different
            // problem and idleness is least of their concerns.
            // 如果 lastChangeCheckTimeStamp 和 lastWriteTime 不一样，说明写操作进行过了，则更新此值
            if (lastChangeCheckTimeStamp != lastWriteTime) {
                lastChangeCheckTimeStamp = lastWriteTime;

                /**
                 * 第 14 至 17 行：这段逻辑，理论来说不会发生。因为 lastWriteTime 属性，只会在 writeListener 回调中修改，
                 * 那么如果发生 lastChangeCheckTimeStamp 和 lastWriteTime 不相等，first 必然为 true 。
                 * 因为，Channel 相关的事件逻辑，都在它所在的 EventLoop 中，
                 * 不会出现并发的情况。关于这一块，【莫那一鲁道】在 https://github.com/netty/netty/issues/8251 已经进行提问，坐等结果
                 */

                // But this applies only if it's the non-first call.
                if (!first) { // 非首次
                    return true;
                }
            }

            Channel channel = ctx.channel();
            Unsafe unsafe = channel.unsafe();
            ChannelOutboundBuffer buf = unsafe.outboundBuffer();

            if (buf != null) {
                // 获得新的 messageHashCode 和 pendingWriteBytes
                int messageHashCode = System.identityHashCode(buf.current());
                long pendingWriteBytes = buf.totalPendingWriteBytes();
                // 发生了变化
                /**
                 * messageHashCode != lastMessageHashCode 成立，① 有可能对端接收数据比较慢，导致一个消息发送了一部分；
                 * ② 又或者，发送的消息非常非常非常大，导致一个消息发送了一部分，就将发送缓存区写满。如果是这种情况下，
                 * 可以使用 ChunkedWriteHandler ，一条大消息，拆成多条小消息。
                 *
                 * pendingWriteBytes != lastPendingWriteBytes 成立，
                 * ① 有新的消息，写到 ChannelOutboundBuffer 内存队列中；
                 * ② 有几条消息成功写到对端。这种情况，此处不会发生
                 */

                if (messageHashCode != lastMessageHashCode || pendingWriteBytes != lastPendingWriteBytes) {
                    // 修改最后一次的 lastMessageHashCode 和 lastPendingWriteBytes
                    lastMessageHashCode = messageHashCode;
                    lastPendingWriteBytes = pendingWriteBytes;
                    /**
                     * 这是一个有点“神奇”的设定，笔者表示不太理解。理论来说，ChannelOutboundBuffer 是否发生变化，只需要考虑【第 30 行】代码的判断。如果加了 !first 的判断，导致的结果是在 WriterIdleTimeoutTask 和 AllIdleTimeoutTask 任务中，ChannelOutboundBuffer 即使发生了变化，在首次还是会触发 write 和 all 空闲事件，在非首次不会触发 write 和 all 空闲事件。
                     * 关于上述的困惑，《Netty 那些事儿 ——— 关于 “Netty 发送大数据包时 触发写空闲超时” 的一些思考》 一文的作者，也表达了相同的困惑。后续，找闪电侠面基沟通下。
                     * 关于上述的困惑，《Netty 心跳服务之 IdleStateHandler 源码分析》 一文的作者，表达了自己的理解。感兴趣的胖友，可以看看。
                     * 当然，这块如果不理解的胖友，也不要方。从笔者目前了解下来，observeOutput 都是设置为 false 。也就说，不会触发这个方法的执行
                     */

                    if (!first) {// 非首次
                        //表示 ChannelOutboundBuffer 发生变化
                        //
                        return true;
                    }
                }
            }
        }

        //返回 false ，表示 ChannelOutboundBuffer 未发生变化
        return false;
    }

    /**
     * 空闲任务抽象类
     */
    private abstract static class AbstractIdleTask implements Runnable {

        private final ChannelHandlerContext ctx;

        AbstractIdleTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            // <1> 忽略未打开的 Channel
            if (!ctx.channel().isOpen()) {
                return;
            }
            // <2> 执行任务
            run(ctx);
        }

        protected abstract void run(ChannelHandlerContext ctx);
    }

    /**
     * 检测 Read 空闲超时定时任务
     */
    private final class ReaderIdleTimeoutTask extends AbstractIdleTask {

        ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            // 计算下一次检测的定时任务的延迟
            long nextDelay = readerIdleTimeNanos;
            //reading 为 true 时，意味着正在读取，不会被检测为读空闲
            if (!reading) {

                //reading 为 false 时，实际 nextDelay 的计算为 readerIdleTimeNanos - (ticksInNanos() - lastReadTime) 。
                // 如果小于等于 0 ，意味着 ticksInNanos() - lastReadTime >= readerIdleTimeNanos ，超时
                nextDelay -= ticksInNanos() - lastReadTime;
            }
            // 如果小于等于 0 ，说明检测到读空闲
            if (nextDelay <= 0) {
                // 延迟时间为 readerIdleTimeNanos ，即再次检测 其中，延迟时间为 readerIdleTimeNanos ，即重新计数
                // Reader is idle - set a new timeout and notify the callback.
                readerIdleTimeout = schedule(ctx, this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);
                // 获得当前是否首次检测到读空闲
                boolean first = firstReaderIdleEvent;
                // 标记 firstReaderIdleEvent 为 false 。也就说，下次检测到空闲，就非首次了。
                firstReaderIdleEvent = false;

                try {
                    // 创建读空闲事件
                    IdleStateEvent event = newIdleStateEvent(IdleState.READER_IDLE, first);
                    // 通知通道空闲事件
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    // 触发 Exception Caught 到下一个节点
                    ctx.fireExceptionCaught(t);
                }
                // 如果大于 0 ，说明未检测到读空闲
            } else {
                // 延迟时间为 nextDelay ，即按照最后一次读的时间作为开始计数
                // Read occurred before the timeout - set a new timeout with shorter delay.
                //初始下一次的 ReaderIdleTimeoutTask 定时任务。其中，延迟时间为 nextDelay ，即按照最后一次读的时间作为开始计数
                readerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private final class WriterIdleTimeoutTask extends AbstractIdleTask {

        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            // 计算下一次检测的定时任务的延迟
            long lastWriteTime = IdleStateHandler.this.lastWriteTime;
            long nextDelay = writerIdleTimeNanos - (ticksInNanos() - lastWriteTime);
            // 如果小于等于 0 ，说明检测到写空闲
            if (nextDelay <= 0) {
                // 延迟时间为 writerIdleTimeout ，即再次检测
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeout = schedule(ctx, this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
                // 获得当前是否首次检测到写空闲
                boolean first = firstWriterIdleEvent;
                // 标记 firstWriterIdleEvent 为 false 。也就说，下次检测到空闲，就非首次了。
                firstWriterIdleEvent = false;

                try {
                    // 判断 ChannelOutboundBuffer 是否发生变化
                    if (hasOutputChanged(ctx, first)) {
                        return;
                    }
                    // 创建写空闲事件
                    IdleStateEvent event = newIdleStateEvent(IdleState.WRITER_IDLE, first);
                    // 通知通道空闲事件
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    // 触发 Exception Caught 到下一个节点
                    ctx.fireExceptionCaught(t);
                }
                // 如果大于 0 ，说明未检测到读空闲
            } else {
                // Write occurred before the timeout - set a new timeout with shorter delay.
                writerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private final class AllIdleTimeoutTask extends AbstractIdleTask {

        AllIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            // 计算下一次检测的定时任务的延迟
            long nextDelay = allIdleTimeNanos;
            if (!reading) {
               // 取 lastReadTime 和 lastWriteTime 中的大值，从而来判断，是否有 Write 和 Read 任一一种空闲。
                nextDelay -= ticksInNanos() - Math.max(lastReadTime, lastWriteTime);
            }
            // 如果小于等于 0 ，说明检测到 all 空闲
            if (nextDelay <= 0) {
                // 延迟时间为 allIdleTimeNanos ，即再次检测
                // Both reader and writer are idle - set a new timeout and
                // notify the callback.
                allIdleTimeout = schedule(ctx, this, allIdleTimeNanos, TimeUnit.NANOSECONDS);
                // 获得当前是否首次检测到 all 空闲
                boolean first = firstAllIdleEvent;
                // 标记 firstAllIdleEvent 为 false 。也就说，下次检测到空闲，就非首次了。
                firstAllIdleEvent = false;

                try {
                    // 判断 ChannelOutboundBuffer 是否发生变化
                    if (hasOutputChanged(ctx, first)) {
                        return;
                    }
                    // 创建 all 空闲事件
                    IdleStateEvent event = newIdleStateEvent(IdleState.ALL_IDLE, first);
                    // 通知通道空闲事件
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
                // 如果大于 0 ，说明未检测到 all 空闲
            } else {
                // Either read or write occurred before the timeout - set a new
                // timeout with shorter delay.
                allIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}
