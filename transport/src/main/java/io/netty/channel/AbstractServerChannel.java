package io.netty.channel;

import java.net.SocketAddress;

/**
 * A skeletal httpserver-side {@link Channel} implementation.  A httpserver-side
 * {@link Channel} does not allow the following operations:
 * <ul>
 * <li>{@link #connect(SocketAddress, ChannelPromise)}</li>
 * <li>{@link #disconnect(ChannelPromise)}</li>
 * <li>{@link #write(Object, ChannelPromise)}</li>
 * <li>{@link #flush()}</li>
 * <li>and the shortcut methods which calls the methods mentioned above
 * </ul>
 */
public abstract class AbstractServerChannel extends AbstractChannel implements ServerChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);

    /**
     * Creates a new instance.
     */
    protected AbstractServerChannel() {
        super(null);
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new DefaultServerUnsafe();
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) {
        throw new UnsupportedOperationException();
    }

    private final class DefaultServerUnsafe extends AbstractUnsafe {
        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            safeSetFailure(promise, new UnsupportedOperationException());
        }
    }
}
