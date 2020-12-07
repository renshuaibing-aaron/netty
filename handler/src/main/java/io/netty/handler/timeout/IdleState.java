package io.netty.handler.timeout;

import io.netty.channel.Channel;


/**
 * 空闲状态枚举
 * An {@link Enum} that represents the idle state of a {@link Channel}.
 */
public enum IdleState {
    /**
     * 读空闲
     * No data was received for a while.
     */
    READER_IDLE,
    /**
     * 写空闲
     * No data was sent for a while.
     */
    WRITER_IDLE,
    /**
     * 读或写任一空闲
     * No data was either received or sent for a while.
     */
    ALL_IDLE
}
