1.什么是JDK的空轮询bug    什么后果  netty是怎么处理的？
正常情况下 selector.select()操作是阻塞的 只有被监听的fd有读写时 才会被唤醒
但是在这个bug中 没有任何的fd读写请求 但是select()操作依旧被唤醒
这种情况下 selectKeys() 返回的是个空数组
然后按照逻辑执行到while(true) 死循环 导致CPU升高

在netty种怎么定义空轮询 select()操作执行了0毫秒

解决办法
netty重新建立selector来解决  判断是否是其他线程发起的重建请求，若不是则将原 SocketChannel 从旧的 Selector 上取消注册，然后重新注册到新的
 Selector 上，最后将原来的 Selector 关闭