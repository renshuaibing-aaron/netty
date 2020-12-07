package io.netty.example.aaron.httpserver.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.example.aaron.rpc.ClientBootstrap;
import io.netty.example.aaron.rpc.HelloServiceImpl;

@ChannelHandler.Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        System.out.println("=====服务器货物参数========="+msg.toString());

        ctx.writeAndFlush(msg);
    }
}
