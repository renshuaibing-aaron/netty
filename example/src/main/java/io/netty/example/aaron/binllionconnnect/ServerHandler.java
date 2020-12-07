package io.netty.example.aaron.binllionconnnect;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    //这里用来对连接数进行记数,每两秒输出到控制台
    private static final AtomicInteger nConnection = new AtomicInteger();

    static {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.out.println("connections: " + nConnection.get());
        }, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        nConnection.incrementAndGet();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        nConnection.decrementAndGet();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf=(ByteBuf) msg;
        byte[] req=new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body=new String (req,"UTF-8");
        String reqString ="Hello I am Server";

        Thread.sleep(10000);
        System.out.println(body + "" + Thread.currentThread().getName());
        System.out.println("EchoInServerHandler printing");
        ByteBuf resp= Unpooled.copiedBuffer(reqString.getBytes());
        ctx.writeAndFlush(resp);

    }

}