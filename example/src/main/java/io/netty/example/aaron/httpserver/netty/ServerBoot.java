package io.netty.example.aaron.httpserver.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class ServerBoot {


    public static NettyServerHandler serverHandler = new NettyServerHandler();

    public static void main(String[] args) throws InterruptedException {


        NioEventLoopGroup bossGroup = new NioEventLoopGroup();

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();


        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new StringDecoder());
                        p.addLast(new StringEncoder());
                        p.addLast(serverHandler);
                    }
                });


        ChannelFuture sync = serverBootstrap.bind("127.0.0.1", 8888).sync();

        sync.channel().closeFuture().sync();
    }
}
