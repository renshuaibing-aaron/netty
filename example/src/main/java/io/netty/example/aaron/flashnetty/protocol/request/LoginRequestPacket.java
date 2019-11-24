package io.netty.example.aaron.flashnetty.protocol.request;

import io.netty.example.aaron.flashnetty.protocol.Packet;
import lombok.Data;

import static io.netty.example.aaron.flashnetty.protocol.command.Command.LOGIN_REQUEST;

@Data
public class LoginRequestPacket extends Packet {
    private String userName;

    private String password;

    @Override
    public Byte getCommand() {

        return LOGIN_REQUEST;
    }
}
