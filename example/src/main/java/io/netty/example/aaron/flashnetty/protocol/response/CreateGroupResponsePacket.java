package io.netty.example.aaron.flashnetty.protocol.response;

import io.netty.example.aaron.flashnetty.protocol.Packet;
import lombok.Data;

import java.util.List;

import static io.netty.example.aaron.flashnetty.protocol.command.Command.CREATE_GROUP_RESPONSE;

@Data
public class CreateGroupResponsePacket extends Packet {
    private boolean success;

    private String groupId;

    private List<String> userNameList;

    @Override
    public Byte getCommand() {

        return CREATE_GROUP_RESPONSE;
    }
}
