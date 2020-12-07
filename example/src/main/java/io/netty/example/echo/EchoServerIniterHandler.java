package io.netty.example.echo;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class EchoServerIniterHandler  extends ChannelInitializer<SocketChannel> {

    //什么时候执行
   @Override
    public void initChannel(SocketChannel ch) throws Exception {

       System.out.println("=====EchoServerIniterHandler#initChannel===============");
        ChannelPipeline p = ch.pipeline();

        //为什么out需要放前面
        p.addLast(new EchoOutServerHandler())
         .addLast(new EchoInServerHandler());
    }
}
