package io.netty.example.aaron.flashnetty.protocol.request;

import io.netty.example.aaron.flashnetty.protocol.Packet;
import lombok.Data;

import java.util.List;

import static io.netty.example.aaron.flashnetty.protocol.command.Command.CREATE_GROUP_REQUEST;

@Data
public class CreateGroupRequestPacket extends Packet {

    private List<String> userIdList;

    @Override
    public Byte getCommand() {

        return CREATE_GROUP_REQUEST;
    }
}
