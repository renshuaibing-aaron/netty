package io.netty.example.aaron.flashnetty.protocol.request;

import io.netty.example.aaron.flashnetty.protocol.Packet;
import lombok.Data;

import static io.netty.example.aaron.flashnetty.protocol.command.Command.LOGOUT_REQUEST;

@Data
public class LogoutRequestPacket extends Packet {
    @Override
    public Byte getCommand() {

        return LOGOUT_REQUEST;
    }
}
