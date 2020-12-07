package io.netty.example.aaron.zerocopy.common.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * 内存分配器.
 *
 * @author cxs
 */
public class MemoryAllocator {

    private static final ThreadLocal<Map<Integer, ByteBuffer>> MEMORY_POOL = ThreadLocal.withInitial(HashMap::new);

    /**
     * 分配内存.
     * @param capacity
     * @return
     */
    public static ByteBuffer allocate(int capacity) {
        Map<Integer, ByteBuffer> cache = MEMORY_POOL.get();
        ByteBuffer byteBuffer = cache.get(capacity);
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocateDirect(capacity);
            cache.put(capacity, byteBuffer);
        }

        return byteBuffer;
    }

    /**
     * 回收内存.
     * @param byteBuffer
     */
    public static void recycle(ByteBuffer byteBuffer) {
        int capacity = byteBuffer.capacity();
        byteBuffer.clear();
        MEMORY_POOL.get().put(capacity, byteBuffer);
    }
}
