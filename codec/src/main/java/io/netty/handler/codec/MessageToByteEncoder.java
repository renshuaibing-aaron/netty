package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;


/**
 * {@link ChannelOutboundHandlerAdapter} which encodes message in a stream-like fashion from one message to an
 * {@link ByteBuf}.
 *
 *
 * Example implementation which encodes {@link Integer}s to a {@link ByteBuf}.
 *
 * <pre>
 *     public class IntegerEncoder extends {@link MessageToByteEncoder}&lt;{@link Integer}&gt; {
 *         {@code @Override}
 *         public void encode({@link ChannelHandlerContext} ctx, {@link Integer} msg, {@link ByteBuf} out)
 *                 throws {@link Exception} {
 *             out.writeInt(msg);
 *         }
 *     }
 * </pre>
 * 编码实现了out接口
 * 负责将消息编码成字节
 */
public abstract class MessageToByteEncoder<I> extends ChannelOutboundHandlerAdapter {

    private final TypeParameterMatcher matcher;
    private final boolean preferDirect;

    /**
     * see {@link #MessageToByteEncoder(boolean)} with {@code true} as boolean parameter.
     */
    protected MessageToByteEncoder() {
        this(true);
    }

    /**
     * see {@link #MessageToByteEncoder(Class, boolean)} with {@code true} as boolean value.
     */
    protected MessageToByteEncoder(Class<? extends I> outboundMessageType) {
        this(outboundMessageType, true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param preferDirect          {@code true} if a direct {@link ByteBuf} should be tried to be used as target for
     *                              the encoded messages. If {@code false} is used it will allocate a heap
     *                              {@link ByteBuf}, which is backed by an byte array.
     */
    protected MessageToByteEncoder(boolean preferDirect) {
        matcher = TypeParameterMatcher.find(this, MessageToByteEncoder.class, "I");
        this.preferDirect = preferDirect;
    }

    /**
     * Create a new instance
     *
     * @param outboundMessageType   The type of messages to match
     * @param preferDirect          {@code true} if a direct {@link ByteBuf} should be tried to be used as target for
     *                              the encoded messages. If {@code false} is used it will allocate a heap
     *                              {@link ByteBuf}, which is backed by an byte array.
     */
    protected MessageToByteEncoder(Class<? extends I> outboundMessageType, boolean preferDirect) {
        matcher = TypeParameterMatcher.get(outboundMessageType);
        this.preferDirect = preferDirect;
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     */
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    /**
     * 注意 这个地方Response 对象，从 BusinessHandler 传入到 MessageToByteEncoder的时候，首先是传入到 write 方法
     * @param ctx
     * @param msg
     * @param promise
     * @throws Exception
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {


        ByteBuf buf = null;
        try {
            //判断当前的encode是否可以处理这个对象
            if (acceptOutboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                        //强制类型转换
                I cast = (I) msg;
                //内存分配  这里注意 默认是DirectByteBuf
                buf = allocateBuffer(ctx, cast, preferDirect);
                try {
                    //编码的具体实现  有用户自己去实现  注意这个地方才开始回调用户自己编写的encode 方法


                    //其实编码的本质就是 把一个java对象序列化后byte[] 然后传入ByteBuf的过程
                    //todo 对比学习public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {
                    //便于学习请查看 rocketMq的编码类NettyEncoder
                    encode(ctx, cast, buf);
                } finally {
                    //释放原始对象 因为自定义的java对象 已经转换成了ByteBuf了 对象已经无用了 释放即可
                    //这里需要注意 只有原始对象是ReferenceCounted的实现类才释放
                    // (当传入的msg类型是ByteBuf的时候，就不需要自己手动释放了)
                    ReferenceCountUtil.release(cast);
                }

                //编码过程中已经写入一些数据
                if (buf.isReadable()) {
                    //调用 往前传播 最终会传入到head 节点 在HeadContext 节点写出去
                    ctx.write(buf, promise);
                } else {
                    //如果没有byteBuf中没有数据 释放该对象 然后传递一个空的对象
                    buf.release();
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                buf = null;
            } else {
                //如果不能进行编码 直接往下进行传递
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            //释放内存
            if (buf != null) {
                //释放开始的内存
                buf.release();
            }
        }
    }

    /**
     * Allocate a {@link ByteBuf} which will be used as argument of {@link #encode(ChannelHandlerContext, I, ByteBuf)}.
     * Sub-classes may override this method to return {@link ByteBuf} with a perfect matching {@code initialCapacity}.
     */
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, @SuppressWarnings("unused") I msg,
                               boolean preferDirect) throws Exception {
        if (preferDirect) {
            return ctx.alloc().ioBuffer();  //堆外内存
        } else {
            return ctx.alloc().heapBuffer();
        }
    }

    /**
     * Encode a message into a {@link ByteBuf}. This method will be called for each written message that can be handled
     * by this encoder.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
     * @param msg           the message to encode
     * @param out           the {@link ByteBuf} into which the encoded message will be written
     * @throws Exception    is thrown if an error occurs
     */
    protected abstract void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception;

    protected boolean isPreferDirect() {
        return preferDirect;
    }
}
