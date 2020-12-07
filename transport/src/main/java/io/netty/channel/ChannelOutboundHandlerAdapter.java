package io.netty.channel;

import java.net.SocketAddress;

/**
 * Skeleton implementation of a {@link ChannelOutboundHandler}. This implementation just forwards each method call via
 * the {@link ChannelHandlerContext}.
 */
public class ChannelOutboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelOutboundHandler {

    /**
     * Calls {@link ChannelHandlerContext#bind(SocketAddress, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
            ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#connect(SocketAddress, SocketAddress, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
            SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#disconnect(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.disconnect(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#close(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.close(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#deregister(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#read()} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    /**
     * Calls {@link ChannelHandlerContext#write(Object, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#flush()} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
