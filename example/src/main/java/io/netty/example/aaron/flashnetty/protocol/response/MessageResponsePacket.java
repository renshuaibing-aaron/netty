package io.netty.example.aaron.flashnetty.protocol.response;

import io.netty.example.aaron.flashnetty.protocol.Packet;
import lombok.Data;

import static io.netty.example.aaron.flashnetty.protocol.command.Command.MESSAGE_RESPONSE;

@Data
public class MessageResponsePacket extends Packet {

    private String fromUserId;

    private String fromUserName;

    private String message;

    @Override
    public Byte getCommand() {

        return MESSAGE_RESPONSE;
    }
}
