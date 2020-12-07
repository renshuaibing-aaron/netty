package io.netty.example.aaron.threadmodel;

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

        MasterReactor();
    }



    public static void MasterReactor() throws Exception {
        EventLoopGroup bossGroup = null;
        EventLoopGroup workerGroup = null;
        EventLoopGroup bus = null;

        try {

            // Configure the httpserver.
            ServerBootstrap b = new ServerBootstrap();

            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(1);
            bus = new NioEventLoopGroup(10);

            EventLoopGroup finalBus = bus;
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                          ch.pipeline().addLast(finalBus,new EchoInServerHandler());;
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


}
