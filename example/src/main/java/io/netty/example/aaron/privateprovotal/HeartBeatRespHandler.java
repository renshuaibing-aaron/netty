package io.netty.example.aaron.privateprovotal;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class HeartBeatRespHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.HEARTBEAT_REQ.value) {
            System.out.println("server:receive client HeartBeat ---> " + message);
            NettyMessage heartBeat = buildHeartBeat();
            ctx.writeAndFlush(heartBeat);
            System.out.println("server:send  HeartBeat to client ---> " + heartBeat);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private NettyMessage buildHeartBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.HEARTBEAT_RESP.value);
        message.setHeader(header);
        message.setBody((byte) 0);
        return message;
    }
}