package io.netty.example.aaron.takelongtime.Context;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class EchoServerHandler extends SimpleChannelInboundHandler<String> {
    public static final ChannelHandler INSTANCE = new EchoServerHandler();
    private static final String LINE = System.getProperty("line.separator");

    private EchoServerHandler() {
    }
    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) {
        //模拟耗时任务
        try {
            Thread.sleep(5000);
            System.out.printf("%s execute time 5s \n", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ctx.writeAndFlush(msg + LINE).addListener(ChannelFutureListener.CLOSE);
    }
}
