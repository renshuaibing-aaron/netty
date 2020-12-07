package io.netty.example.aaron.binllionconnnect;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.CountDownLatch;

public class Client {

    //服务端的IP
    private static final String SERVER_HOST = "127.0.0.1";

    static final int BEGIN_PORT = 10000;
    static final int N_PORT = 100;

    public static void main(String[] args) {
        new Client().start(BEGIN_PORT, N_PORT);
    }

    public void start(final int beginPort, int nPort) {
        long start = System.currentTimeMillis();
        System.out.println("client starting...."+System.currentTimeMillis());
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new ClientCountHandler());
            }
        });

        int index = 0;
        int port=0;
        //从10000的端口开始，按端口递增的方式进行连接
        boolean flag=true;
        while (port<10000+10) {
            port = beginPort + index;
            System.out.println("===port==="+port);
            try {
                ChannelFuture channelFuture = bootstrap.connect(SERVER_HOST, port);
                channelFuture.addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        System.out.println("========达到最大连接数================!");
                        System.out.println(Thread.currentThread());
                        Thread.currentThread().interrupt();
                    }
                });
                channelFuture.get();
            } catch (Exception e) {
                System.out.println("----------线程中断----------");
            }

            if (++index == nPort) {
                index = 0;
            }
        }


        long end = System.currentTimeMillis();
        try {
            System.out.println("======等待==============="+(start-end));
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}