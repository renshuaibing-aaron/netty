package io.netty.example.aaron.flashnetty.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.example.aaron.flashnetty.protocol.Packet;
import io.netty.example.aaron.flashnetty.protocol.PacketCodeC;
import io.netty.example.aaron.flashnetty.protocol.request.LoginRequestPacket;
import io.netty.example.aaron.flashnetty.serialize.Serializer;
import io.netty.example.aaron.flashnetty.serialize.impl.JSONSerializer;
import org.junit.Assert;
import org.junit.Test;

public class PacketCodeCTest {
    @Test
    public void encode() {

        Serializer serializer = new JSONSerializer();
        LoginRequestPacket loginRequestPacket = new LoginRequestPacket();

        loginRequestPacket.setVersion(((byte) 1));
        loginRequestPacket.setUserName("zhangsan");
        loginRequestPacket.setPassword("password");

        PacketCodeC packetCodeC = PacketCodeC.INSTANCE;
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.ioBuffer();

        packetCodeC.encode(byteBuf, loginRequestPacket);
        Packet decodedPacket = packetCodeC.decode(byteBuf);

        Assert.assertArrayEquals(serializer.serialize(loginRequestPacket), serializer.serialize(decodedPacket));

    }
}
