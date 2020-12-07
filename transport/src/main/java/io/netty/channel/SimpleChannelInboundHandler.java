package io.netty.channel;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

/**
 * {@link ChannelInboundHandlerAdapter} which allows to explicit only handle a specific type of messages.
 *
 * For example here is an implementation which only handle {@link String} messages.
 *
 * <pre>
 *     public class StringHandler extends
 *             {@link SimpleChannelInboundHandler}&lt;{@link String}&gt; {
 *
 *         {@code @Override}
 *         protected void channelRead0({@link ChannelHandlerContext} ctx, {@link String} message)
 *                 throws {@link Exception} {
 *             System.out.println(message);
 *         }
 *     }
 * </pre>
 *
 * Be aware that depending of the constructor parameters it will release all handled messages by passing them to
 * {@link ReferenceCountUtil#release(Object)}. In this case you may need to use
 * {@link ReferenceCountUtil#retain(Object)} if you pass the object to the next handler in the {@link ChannelPipeline}.
 *
 * <h3>Forward compatibility notice</h3>
 * <p>
 * Please keep in mind that {@link #channelRead0(ChannelHandlerContext, I)} will be renamed to
 * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.
 * </p>
 */
public abstract class SimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter {

    private final TypeParameterMatcher matcher;
    private final boolean autoRelease;

    /**
     * see {@link #SimpleChannelInboundHandler(boolean)} with {@code true} as boolean parameter.
     */
    protected SimpleChannelInboundHandler() {
        this(true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param autoRelease   {@code true} if handled messages should be released automatically by passing them to
     *                      {@link ReferenceCountUtil#release(Object)}.
     */
    protected SimpleChannelInboundHandler(boolean autoRelease) {
        matcher = TypeParameterMatcher.find(this, SimpleChannelInboundHandler.class, "I");
        this.autoRelease = autoRelease;
    }

    /**
     * see {@link #SimpleChannelInboundHandler(Class, boolean)} with {@code true} as boolean value.
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType) {
        this(inboundMessageType, true);
    }

    /**
     * Create a new instance
     *
     * @param inboundMessageType    The type of messages to match
     * @param autoRelease           {@code true} if handled messages should be released automatically by passing them to
     *                              {@link ReferenceCountUtil#release(Object)}.
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType, boolean autoRelease) {
        matcher = TypeParameterMatcher.get(inboundMessageType);
        this.autoRelease = autoRelease;
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    /**
     * todo 模板方法的使用 可以解决内存泄露问题
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("------SimpleChannelInboundHandler#channelRead  这里主要可以进行手动释放内存-----------");
        boolean release = true;
        try {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                I imsg = (I) msg;
                channelRead0(ctx, imsg);
            } else {
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (autoRelease && release) {
                System.out.println("todo 模板方法的使用 可以解决内存泄露问题");
                ReferenceCountUtil.release(msg);
            }
        }
    }

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     *
     * Is called for each message of type {@link I}.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     * @throws Exception    is thrown if an error occurred
     */
    protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;
}
