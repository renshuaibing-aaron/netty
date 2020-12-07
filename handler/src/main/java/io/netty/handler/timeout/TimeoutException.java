
package io.netty.handler.timeout;

import io.netty.channel.ChannelException;

/**
 * 继承 ChannelException 类，超时异常
 * A {@link TimeoutException} when no data was either read or written within a
 * certain period of time.
 */
public class TimeoutException extends ChannelException {

    private static final long serialVersionUID = 4673641882869672533L;

    TimeoutException() { }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
