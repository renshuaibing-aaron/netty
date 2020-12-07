package io.netty.example.aaron.fastthreadlocal;

import io.netty.util.Recycler;

public class RecycleTest {
    private static final Recycler<User> RECYCLE = new Recycler() {
        @Override
        protected User newObject(Handle handle) {
            return new User(handle);
        }
    };

    private static class User {
        private final Recycler.Handle<User> handle;

        public User(Recycler.Handle<User> handle) {
            this.handle = handle;
        }

        public void recycle() {
            handle.recycle(this);
        }
    }


    public static void main(String[] args) {
        User user = RECYCLE.get();
        user.recycle();

        User user1 = RECYCLE.get();

        System.out.println(user.equals(user1));
    }
}
