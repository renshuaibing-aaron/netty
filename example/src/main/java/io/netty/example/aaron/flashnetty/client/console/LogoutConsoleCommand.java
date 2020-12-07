package io.netty.example.aaron.flashnetty.client.console;

import io.netty.channel.Channel;
import io.netty.example.aaron.flashnetty.protocol.request.LogoutRequestPacket;

import java.util.Scanner;

public class LogoutConsoleCommand implements ConsoleCommand {
    @Override
    public void exec(Scanner scanner, Channel channel) {
        LogoutRequestPacket logoutRequestPacket = new LogoutRequestPacket();
        channel.writeAndFlush(logoutRequestPacket);
    }
}
