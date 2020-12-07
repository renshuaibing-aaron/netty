package io.netty.example.aaron.reactor.threadpool;

import io.netty.example.aaron.reactor.single.Handler;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Acceptor implements Handler {

    private final Selector selector;

    private final ServerSocketChannel serverSocketChannel;

    Acceptor(ServerSocketChannel serverSocketChannel, Selector selector) {
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
    }

    @Override
    public void handle() {
        SocketChannel socketChannel;
        try {
            socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                System.out.println(String.format("收到来自 %s 的连接",
                        socketChannel.getRemoteAddress()));

                //这里把客户端通道传给Handler，Handler负责接下来的事件处理（除了连接事件以外的事件均可）
                //这里是什么时候启动的？？？
                new AsyncHandler(socketChannel, selector);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}