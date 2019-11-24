package io.netty.example.aaron.nettydemo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public  class SimpleServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("========channelActive=============");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("==========channelRegistered===========");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //这里是干嘛的
        System.out.println("======handlerAdded==========");
    }
}
