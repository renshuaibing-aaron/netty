package io.netty.channel.socket.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.util.internal.SocketUtils;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

/**
 * A {@link io.netty.channel.socket.ServerSocketChannel} implementation which uses
 * NIO selector based implementation to accept new connections.
 */
public class NioServerSocketChannel extends AbstractNioMessageChannel
                             implements io.netty.channel.socket.ServerSocketChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioServerSocketChannel.class);

    private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
            /**
             *  Use the {@link SelectorProvider} to open {@link SocketChannel} and so remove condition in
             *  {@link SelectorProvider#provider()} which is called by each ServerSocketChannel.open() otherwise.
             *
             *  See <a href="https://github.com/netty/netty/issues/2308">#2308</a>.
             */
            System.out.println("====ServerSocketChannel===========");
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new ChannelException(
                    "Failed to open a httpserver socket.", e);
        }
    }

    private final ServerSocketChannelConfig config;

    /**
     * Create a new instance
     * 利用反射构造函数
     */
    public NioServerSocketChannel() {

        //newSocket的作用利用SelectorProvider产生一个SocketChannelImpl对象
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }

    /**
     * Create a new instance using the given {@link SelectorProvider}.
     */
    public NioServerSocketChannel(SelectorProvider provider) {
        this(newSocket(provider));
    }

    /**
     * Create a new instance using the given {@link ServerSocketChannel}.
     *
     * 原生的ServerSocketChannel
     */
    public NioServerSocketChannel(ServerSocketChannel channel) {

        // 对于服务端来说，关心的是 SelectionKey.OP_ACCEPT 事件，等待客户端连接
        super(null, channel, SelectionKey.OP_ACCEPT);
        //设置了config属性
        //这个 config 对象用于配置这个 NioServerSocketChannel ，用于外部获取参数和配置？？
        config = new NioServerSocketChannelConfig(this, javaChannel().socket());
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ServerSocketChannelConfig config() {
        return config;
    }

    @Override
    public boolean isActive() {
        return javaChannel().socket().isBound();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    protected ServerSocketChannel javaChannel() {
        return (ServerSocketChannel) super.javaChannel();
    }

    @Override
    protected SocketAddress localAddress0() {
        return SocketUtils.localSocketAddress(javaChannel().socket());
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        //上面方法中javaChannel()方法返回的是NioServerSocketChannel实例初始化时所产生的Java NIO ServerSocketChannel实例
        // （更具体点为ServerSocketChannelImple实例）。
        // 等价于语句serverSocketChannel.socket().bind(localAddress)完成了指定端口的绑定，这样就开始监听此端口
        if (PlatformDependent.javaVersion() >= 7) {
            javaChannel().bind(localAddress, config.getBacklog());
        } else {
            javaChannel().socket().bind(localAddress, config.getBacklog());
        }
    }

    @Override
    protected void doClose() throws Exception {
        javaChannel().close();
    }

    /**
     * doReadMessage  其实就是 doChannel
     * 处理新连接, 现在是在 NioServReaderSocketChannel里面
     * @param buf
     * @return
     * @throws Exception
     */
    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {

        //javaChannel()  服务端启动过程中 产生的jdk channel
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {
                //   把java原生的channel, 封装成 Netty自定义的封装的channel , 这里的buf是list集合对象,由上一层传递过来的
                //   this  --  NioServerSocketChannel
                //   ch --     SocketChnnel
                //todo  为啥,服务端的channel需要反射创建,而客户的的channel直接new
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }

    // Unnecessary stuff
    @Override
    protected boolean doConnect(
            SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doFinishConnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) throws Exception {
        throw new UnsupportedOperationException();
    }

    private final class NioServerSocketChannelConfig extends DefaultServerSocketChannelConfig {
        private NioServerSocketChannelConfig(NioServerSocketChannel channel, ServerSocket javaSocket) {
            super(channel, javaSocket);
        }

        @Override
        protected void autoReadCleared() {
            clearReadPending();
        }
    }
}
