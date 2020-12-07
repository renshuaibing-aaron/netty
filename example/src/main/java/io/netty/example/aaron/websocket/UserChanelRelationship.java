package io.netty.example.aaron.websocket;

import io.netty.channel.Channel;

import java.util.HashMap;

public class UserChanelRelationship {
    private static HashMap<String, Channel> manager = new HashMap<>();

    public static void put(String sendId, Channel channel) {
        manager.put(sendId, channel);
    }

    public static Channel get(String sendId) {
        return manager.get(sendId);
    }

    public static void outPut() {
        for (HashMap.Entry<String, Channel> entry : manager.entrySet()) {
            System.out.println("UserId: " + entry.getKey() + "channelId: " + entry.getValue().id().asLongText());
        }
    }
}