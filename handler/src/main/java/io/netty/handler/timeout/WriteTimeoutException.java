package io.netty.handler.timeout;

/**
 * 继承 TimeoutException 类，写超时( 空闲 )异常。代码如下
 * A {@link TimeoutException} raised by {@link WriteTimeoutHandler} when no data
 * was written within a certain period of time.
 */
public final class WriteTimeoutException extends TimeoutException {

    private static final long serialVersionUID = -144786655770296065L;

    /**
     * 单例
     */
    public static final WriteTimeoutException INSTANCE = new WriteTimeoutException();

    private WriteTimeoutException() { }
}
