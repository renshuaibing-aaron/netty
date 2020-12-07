package io.netty.example.aaron.zerocopy.server;

import io.netty.example.aaron.zerocopy.common.RpcPacket;

import java.nio.channels.SocketChannel;

/**
 * 回复给 socketChannel 的映射包.
 */
class ReplyPacket {

    private SocketChannel socketChannel;
    private RpcPacket rpcPacket;

    public ReplyPacket(SocketChannel socketChannel, RpcPacket rpcPacket) {
        this.socketChannel = socketChannel;
        this.rpcPacket = rpcPacket;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public RpcPacket getRpcPacket() {
        return rpcPacket;
    }

    public void setRpcPacket(RpcPacket rpcPacket) {
        this.rpcPacket = rpcPacket;
    }
}
