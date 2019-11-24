package io.netty.example.aaron.flashnetty.protocol.response;

import io.netty.example.aaron.flashnetty.protocol.Packet;
import lombok.Data;

import static io.netty.example.aaron.flashnetty.protocol.command.Command.LOGIN_RESPONSE;


@Data
public class LoginResponsePacket extends Packet {
    private String userId;

    private String userName;

    private boolean success;

    private String reason;


    @Override
    public Byte getCommand() {

        return LOGIN_RESPONSE;
    }
}
