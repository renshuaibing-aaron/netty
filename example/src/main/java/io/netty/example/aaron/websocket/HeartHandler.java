package io.netty.example.aaron.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @Author: Changwu
 * @Date: 2019/7/2 9:33
 * 我们的心跳handler不需要实现handler0方法,我们选择,直接继承SimpleInboundHandler的父类
 */
public class HeartHandler extends ChannelInboundHandlerAdapter {
    // 我们重写  EventTrigger 方法
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 当出现read/write  读写写空闲时触发
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state() == IdleState.READER_IDLE) { // 读空闲
                System.out.println(ctx.channel().id().asShortText() + " 读空闲... ");
            } else if (event.state() == IdleState.WRITER_IDLE) {
                System.out.println(ctx.channel().id().asShortText() + " 写空闲... ");
            } else if (event.state() == IdleState.ALL_IDLE) {
                System.out.println("channel 读写空闲, 准备关闭当前channel  , 当前UsersChanel的数量: " + MyHandler.users.size());
                Channel channel = ctx.channel();
                channel.close();
                System.out.println("channel 关闭后, UsersChanel的数量: " + MyHandler.users.size());
            }
        }
    }

}