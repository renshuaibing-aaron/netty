package io.netty.example.aaron.privateprovotal;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息头
 * Created by root on 1/11/18.
 */
@Data
public final class Header {
    private int crcCode = 0xabef0101;
    private int length;
    private long sessionID;
    private byte type;//消息类型
    private byte priority;//消息优先级
    private Map<String, Object> attachment = new HashMap<String, Object>();//附件

}


/*
*   4        4       32       1    1
* crcCode  length  sessionID type priority attachment  Object
* 4, 4, -8, 0
*
* maxFrameLength - 发送的数据包最大长度
*  lengthFieldOffset - 长度域偏移量，指的是长度域位于整个数据包字节数组中的下标
* lengthFieldLength - 长度域的自己的字节数长度。
* lengthAdjustment – 长度域的偏移量矫正。 如果长度域的值，除了包含有效数据域的长度外，
* 还包含了其他域（如长度域自身）长度，那么，就需要进行矫正。矫正的值为：包长 - 长度域的值 – 长度域偏移 – 长度域长
*             发送数据包长度 = 长度域的值 + lengthFieldOffset + lengthFieldLength + lengthAdjustment；
*
* x+8=x+8 4+4+ad
*
* initialBytesToStrip – 丢弃的起始字节数。丢弃处于有效数据前面的字节数量。比如前面有4个节点的长度域，则它的值为4。
* */