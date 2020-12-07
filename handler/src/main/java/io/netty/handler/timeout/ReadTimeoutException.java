package io.netty.handler.timeout;

/**
 * 继承 TimeoutException 类，读超时( 空闲 )异常
 * A {@link TimeoutException} raised by {@link ReadTimeoutHandler} when no data
 * was read within a certain period of time.
 */
public final class ReadTimeoutException extends TimeoutException {

    private static final long serialVersionUID = 169287984113283421L;

    /**
     * 单例
     */
    public static final ReadTimeoutException INSTANCE = new ReadTimeoutException();

    private ReadTimeoutException() { }
}
