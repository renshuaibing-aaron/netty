package io.netty.example.aaron.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.logging.log4j.core.util.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    // 用于管理整个客户端的 组
    public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame frame) throws Exception {
        Channel currentChanenl = channelHandlerContext.channel();

        // 1. 获取客户端发送的消息
        String content = frame.text();
        System.out.println("  content:  " + content);

        // 2. 判断不同的消息的类型, 根据不同的类型进行不同的处理
        // 当建立连接时, 第一次open , 初始化channel,将channel和数据库中的用户做一个唯一的关联
       // DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        DataContent dataContent = new DataContent() ;
        Integer action = dataContent.getAction();

        if (action == MsgActionEnum.CHAT.type) {

            // 3. 把聊天记录保存到数据库
            // 4. 同时标记消息的签收状态 [未签收]
            // 5. 从我们的映射中获取接受方的chanel  发送消息
            // 6. 从 chanelGroup中查找 当前的channel是否存在于 group, 只有存在,我们才进行下一步发送
            //  6.1 如果没有接受者用户channel就不writeAndFlush, 等着用户上线后,通过js发起请求拉取未接受的信息
            //  6.2 如果没有接受者用户channel就不writeAndFlush, 可以选择推送

        } else if (action == MsgActionEnum.CONNECT.type) {
            // 当建立连接时, 第一次open , 初始化channel,将channel和数据库中的用户做一个唯一的关联
           // String sendId = dataContent.getChatMsg().getSenderId();
            String sendId = dataContent.getChatMsg();
            UserChanelRelationship.put(sendId, currentChanenl);

        } else if (action == MsgActionEnum.SINGNED.type) {
            // 7. 当用户没有上线时,发送消息的人把要发送的消息持久化在数据库,但是却没有把信息写回到接受者的channel, 把这种消息称为未签收的消息

            // 8. 签收消息, 就是修改数据库中消息的签收状态, 我们和前端约定,前端如何签收消息在上面有提到
            String extend = dataContent.getExtand();
            // 扩展字段在 signed类型代表 需要被签收的消息的id, 用逗号分隔
            String[] msgIdList = extend.split(",");
            List<String> msgIds = new ArrayList<>();
            Arrays.asList(msgIdList).forEach(s -> {
                if (null != s) {
                    msgIds.add(s);
                }
            });
            if (!msgIds.isEmpty() && null != msgIds && msgIds.size() > 0) {
                // 批量签收
            }

        } else if (action == MsgActionEnum.KEEPALIVE.type) {
            // 6. 心跳类型
            System.out.println("收到来自channel 为" + currentChanenl + " 的心跳包... ");
        }

    }

    // handler 添加完成后回调
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // 获取链接, 并且若想要群发的话,就得往每一个channel中写数据, 因此我们得在创建连接时, 把channel保存起来
        System.err.println("handlerAdded");
        users.add(ctx.channel());
    }

    // 用户关闭了浏览器回调
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // 断开连接后, channel会自动移除group
        // 我们主动的关闭进行, channel会被移除, 但是我们如果是开启的飞行模式,不会被移除
        System.err.println("客户端channel被移出: " + ctx.channel().id().asShortText());
        users.remove(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 发生异常关闭channel, 并从ChannelGroup中移除Channel
        ctx.channel().close();
        users.remove(ctx.channel());
    }
}