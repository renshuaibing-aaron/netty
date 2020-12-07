package io.netty.example.aaron.privateprovotal;

import lombok.Data;

/**
 * 消息
 * Created by root on 1/11/18.
 */
@Data
public final class NettyMessage {

    private Header header;
    private Object body;

}