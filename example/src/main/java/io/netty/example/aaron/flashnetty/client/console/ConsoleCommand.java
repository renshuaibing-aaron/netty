package io.netty.example.aaron.flashnetty.client.console;

import io.netty.channel.Channel;

import java.util.Scanner;

public interface ConsoleCommand {
    void exec(Scanner scanner, Channel channel);
}
