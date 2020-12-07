package io.netty.example.aaron.websocket;

public enum MsgActionEnum {

    // 消息action的枚举,这个枚举和后端约定好,统一值
    CONNECT(1),     // 第一次(或重连)初始化连接
    CHAT(2),        // 聊天消息
    SINGNED(3),      // 消息签收
    KEEPALIVE(4),   // 客户端保持心跳
    PULL_FRIEND(5); // 重新拉取好友

    public final int type;

     MsgActionEnum(int type) {
        this.type = type;
    }
}
