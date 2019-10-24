package io.netty.example.aaron.reactor.masterslave;

import java.io.IOException;

public class ServerBoot {
    public static void main(String[] args) {
        try {
            new Thread(new MainReactor(2333)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
