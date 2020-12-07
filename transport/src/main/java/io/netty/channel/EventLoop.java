package io.netty.channel;
import io.netty.util.concurrent.OrderedEventExecutor;

/**
 * Will handle all the I/O operations for a {@link Channel} once registered.
 *
 * One {@link EventLoop} instance will usually handle more than one {@link Channel} but this may depend on
 * implementation details and internals.
 *  EventLoop是一个极为重要的组件，它翻译过来称为事件循环，一个EventLoop将被分配给一个Channel，来负责这个Channel的整个生命周期之内的所有事件
 *注意：一个 EventLoop 将由一个永远都不会改变的Thread 驱动，
 * 同时任务（Runnable 或者 Callable）可以直接提交给 EventLoop 实现，以立即执行或者调度执行。
 * 根据配置和可用核心的不同，可能会创建多个 EventLoop 实例用以优化资源的使用，并且单个 EventLoop 可能会被指派用于服务多个 Channel。
 *
 */
public interface EventLoop extends OrderedEventExecutor, EventLoopGroup {
    @Override
    EventLoopGroup parent();
}
