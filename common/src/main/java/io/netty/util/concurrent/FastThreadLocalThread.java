package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.UnstableApi;

/**
 * A special {@link Thread} that provides fast access to {@link FastThreadLocal} variables.
 */
public class FastThreadLocalThread extends Thread {
    // This will be set to true if we have a chance to wrap the Runnable.
    private final boolean cleanupFastThreadLocals;

    private InternalThreadLocalMap threadLocalMap;

    public FastThreadLocalThread() {
        cleanupFastThreadLocals = false;
    }

    public FastThreadLocalThread(Runnable target) {
        super(FastThreadLocalRunnable.wrap(target));
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(ThreadGroup group, Runnable target) {
        super(group, FastThreadLocalRunnable.wrap(target));
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(String name) {
        super(name);
        cleanupFastThreadLocals = false;
    }

    public FastThreadLocalThread(ThreadGroup group, String name) {
        super(group, name);
        cleanupFastThreadLocals = false;
    }

    public FastThreadLocalThread(Runnable target, String name) {
        super(FastThreadLocalRunnable.wrap(target), name);
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(ThreadGroup group, Runnable target, String name) {
        super(group, FastThreadLocalRunnable.wrap(target), name);
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, FastThreadLocalRunnable.wrap(target), name, stackSize);
        cleanupFastThreadLocals = true;
    }

    /**
     * Returns the internal data structure that keeps the thread-local variables bound to this thread.
     * Note that this method is for internal use only, and thus is subject to change at any time.
     */
    public final InternalThreadLocalMap threadLocalMap() {
        return threadLocalMap;
    }

    /**
     * Sets the internal data structure that keeps the thread-local variables bound to this thread.
     * Note that this method is for internal use only, and thus is subject to change at any time.
     */
    public final void setThreadLocalMap(InternalThreadLocalMap threadLocalMap) {
        this.threadLocalMap = threadLocalMap;
    }

    /**
     * Returns {@code true} if {@link FastThreadLocal#removeAll()} will be called once {@link #run()} completes.
     */
    @UnstableApi
    public boolean willCleanupFastThreadLocals() {
        return cleanupFastThreadLocals;
    }
}
