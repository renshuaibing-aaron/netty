package io.netty.handler.timeout;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;

import java.util.concurrent.TimeUnit;

/**
 * Raises a {@link ReadTimeoutException} when no data was read within a certain
 * period of time.
 *
 * <pre>
 * // The connection is closed when there is no inbound traffic
 * // for 30 seconds.
 *
 * public class MyChannelInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("readTimeoutHandler", new {@link ReadTimeoutHandler}(30);
 *         channel.pipeline().addLast("myHandler", new MyHandler());
 *     }
 * }
 *
 * // Handler should handle the {@link ReadTimeoutException}.
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *     {@code @Override}
 *     public void exceptionCaught({@link ChannelHandlerContext} ctx, {@link Throwable} cause)
 *             throws {@link Exception} {
 *         if (cause instanceof {@link ReadTimeoutException}) {
 *             // do something
 *         } else {
 *             super.exceptionCaught(ctx, cause);
 *         }
 *     }
 * }
 *
 * {@link ServerBootstrap} bootstrap = ...;
 * ...
 * bootstrap.childHandler(new MyChannelInitializer());
 * ...
 * </pre>
 * @see WriteTimeoutHandler
 * @see IdleStateHandler
 * todo ReadTimeoutHandler ，继承 IdleStateHandler 类，当 Channel 的读空闲时间( 读或者写 )太长时，抛出 ReadTimeoutException 异常，并自动关闭该 Channel 。
 *  然后，你可以自定一个 ChannelInboundHandler ，重写 #exceptionCaught(ChannelHandlerContext ctx, Throwable cause) 方法，处理该异常
 */
public class ReadTimeoutHandler extends IdleStateHandler {
    /**
     * Channel 是否关闭
     */
    private boolean closed;

    /**
     * Creates a new instance.
     *
     * @param timeoutSeconds
     *        read timeout in seconds
     */
    public ReadTimeoutHandler(int timeoutSeconds) {
        this(timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Creates a new instance.
     *
     * @param timeout
     *        read timeout
     * @param unit
     *        the {@link TimeUnit} of {@code timeout}
     */
    public ReadTimeoutHandler(long timeout, TimeUnit unit) {
        // 禁用 Write / All 的空闲检测 只根据 timeout 方法参数，开启 Read 的空闲检测。
        super(timeout, 0, 0, unit);
    }

    @Override
    protected final void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        assert evt.state() == IdleState.READER_IDLE;
        readTimedOut(ctx);
    }

    /**
     * Is called when a read timeout was detected.
     */
    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
        if (!closed) {
            // <1> 触发 Exception Caught 事件到 pipeline 中，异常为 ReadTimeoutException
            ctx.fireExceptionCaught(ReadTimeoutException.INSTANCE);
            // <2> 关闭 Channel 通道
            ctx.close();
            // <3> 标记 Channel 为已关闭
            closed = true;
        }
    }
}
