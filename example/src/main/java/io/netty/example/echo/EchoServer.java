package io.netty.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Echoes back any received data from a client.
 */
public final class EchoServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {

        singleReactor();
    }

    /**
     * 单线程Reactor配置
     * @throws Exception
     */
    public static void singleReactor() throws Exception {

            // Configure the httpserver.
            EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);

            //MultithreadEventLoopGroup
            EventLoop bossLoop = eventLoopGroup.next();
            EventLoop workLoop = bossLoop;

            ServerBootstrap b = new ServerBootstrap();


        b.group(bossLoop, workLoop)
                    //NioServerSocketChannel class 对象。会根据这个 class 创建 channel 对象
                    .channel(NioServerSocketChannel.class)

                    //option 方法传入 TCP 参数，放在一个LinkedHashMap 中
                    .option(ChannelOption.SO_BACKLOG, 100)

                    //handler 方法传入一个 handler 中，这个hanlder 只专属于 ServerSocketChannel 而不是 SocketChannel
                    .handler(new LoggingHandler(LogLevel.INFO))

                    //childHandler 传入一个 hanlder ，这个handler 将会在每个客户端连接的时候调用。供 SocketChannel 使用。

                    //修改
                    .childHandler(new EchoServerIniterHandler());

            // Start the httpserver.
            ChannelFuture f = b.bind(PORT).sync();

        System.out.println("=========22222===========");
            // Wait until the httpserver socket is closed.
            f.channel().closeFuture().sync();

    }



    public void threadPoolReactor() throws Exception {
        EventLoopGroup bossGroup = null;
        EventLoopGroup workerGroup = null;
        try {

            // Configure the httpserver.
            EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
            EventLoop bossLoop = eventLoopGroup.next();
            EventLoopGroup workLoopGroup = new NioEventLoopGroup();
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossLoop, workLoopGroup)

                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new EchoInServerHandler());
                        }
                    });

            // Start the httpserver.
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the httpserver socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    public void MasterReactor() throws Exception {
        EventLoopGroup bossGroup = null;
        EventLoopGroup workerGroup = null;
        try {

            // Configure the httpserver.
            ServerBootstrap b = new ServerBootstrap();

            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new EchoInServerHandler());
                        }
                    });

            // Start the httpserver.
            ChannelFuture bind = b.bind(PORT);
            ChannelFuture f = bind.sync();
            ChannelFuture channelFuture = f.channel().closeFuture();
            // Wait until the httpserver socket is closed.
            channelFuture.sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


}
