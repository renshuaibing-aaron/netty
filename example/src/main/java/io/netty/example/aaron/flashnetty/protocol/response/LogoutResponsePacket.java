package io.netty.example.aaron.flashnetty.protocol.response;

import io.netty.example.aaron.flashnetty.protocol.Packet;
import lombok.Data;

import static io.netty.example.aaron.flashnetty.protocol.command.Command.LOGOUT_RESPONSE;

@Data
public class LogoutResponsePacket extends Packet {

    private boolean success;

    private String reason;


    @Override
    public Byte getCommand() {

        return LOGOUT_RESPONSE;
    }
}
