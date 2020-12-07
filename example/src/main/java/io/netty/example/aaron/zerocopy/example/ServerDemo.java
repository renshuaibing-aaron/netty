package io.netty.example.aaron.zerocopy.example;


import io.netty.example.aaron.zerocopy.server.NioSendFileServer;

import java.io.IOException;

public class ServerDemo {

    public static void main(String[] args) throws IOException {
        NioSendFileServer nioSendFileServer = new NioSendFileServer();
        nioSendFileServer.start("localhost", 8083, "/Users/cxs/send_file_copy_dir");
    }
}
