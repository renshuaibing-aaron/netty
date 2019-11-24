package io.netty.example.aaron.nettydemo;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


/**
 * descripiton: 服务器初始化
 *
 * @author: www.iknowba.cn
 * @date: 2018/3/23
 * @time: 15:46
 * @modifier:
 * @since:
 */
public class ServerIniterHandler extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

        System.out.println("=========注册pipe=========");
        //管道注册handler
        ChannelPipeline pipeline = socketChannel.pipeline();
        //解码通道处理   ChannelInboundHandlerAdapter
        pipeline.addLast("decode", new StringDecoder());

        //编码通道处理   ChannelOutboundHandlerAdapter
        pipeline.addLast("encode", new StringEncoder());
        //聊天服务通道处理   ChannelInboundHandler
        pipeline.addLast("chat", new ServerHandler());

        //下面这个任务执行的时候，将不会阻塞IO线程，执行的线程来自 group 线程池
        //验证测试
       // pipeline.addLast(socketChannel.eventLoop(),"handler",new ServerHandler());
    }
}
