package io.netty.example.aaron.fastthreadlocal;

import io.netty.util.concurrent.FastThreadLocal;

public class FastThreadLocalTest {
    private static FastThreadLocal<Object> threadLocal = new FastThreadLocal<Object>() {
        @Override
        protected Object initialValue() throws Exception {
            return new Object();
        }
    };

    public static void main(String[] args) {

        new Thread(() -> {

            while (true) {
                Object object = new Object();
                threadLocal.set(object);
                System.out.println(threadLocal.get());
            }
        }).start();

        new Thread(() -> {
            Object object1 = threadLocal.get();
            while (true) {
                Object object2 = threadLocal.get();
                System.out.println(object1.equals(object2));
            }
        }).start();
    }
}
