package io.netty.example.aaron.threadmodel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the httpserver.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client
 * and httpserver by sending the first message to
 * the httpserver.
 */
public final class EchoClient {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.git
        final SslContext sslCtx;
        if (SSL) {
            sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                     }
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(new EchoClientHandler());
                 }
             });

            for(int i=1;i<=10;i++){
                System.out.println("====连接=========");
                ChannelFuture f = b.connect(HOST, PORT).sync();
                f.addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        System.out.println("========连接失败================!");
                        System.out.println(Thread.currentThread());
                        Thread.currentThread().interrupt();
                    }
                });

                // Start the client.
                // Wait until the connection is closed.
              //  f.channel().closeFuture().sync();
            }
            new CountDownLatch(1).await();

        } finally {
            // Shut down the event loop to terminate all threads.
            System.out.println("===========优雅关闭========");
            group.shutdownGracefully();
        }



    }
}
