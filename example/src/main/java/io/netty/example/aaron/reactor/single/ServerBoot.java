package io.netty.example.aaron.reactor.single;

import java.io.IOException;

public class ServerBoot {
    public static void main(String[] args) {
        try {
            new Thread(new Reactor(2333)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
