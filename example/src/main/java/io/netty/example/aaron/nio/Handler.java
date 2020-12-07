package io.netty.example.aaron.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by zhangyiwen on 16/11/8.
 */
public interface Handler {

    void handle(SelectionKey sk) throws IOException, InterruptedException;
}