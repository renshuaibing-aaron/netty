package io.netty.example.aaron.privateprovotal;

public enum MessageType {

    //心跳请求，应答
    HEARTBEAT_REQ((byte) 5),
    HEARTBEAT_RESP((byte) 6),

    //握手请求，应答
    LOGIN_REQ((byte) 3),
    LOGIN_RESP((byte) 4);

    byte value;

    MessageType(byte value) {
        this.value = value;
    }
}