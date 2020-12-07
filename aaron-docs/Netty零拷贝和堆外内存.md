支撑百万并发的零拷贝技术是怎么实现的？
和堆外内存的关系？
https://mp.weixin.qq.com/s?__biz=MzU0OTk3ODQ3Ng==&mid=2247486394&idx=1&sn=0836816b044d041018a239bf4c3718e8&chksm=fba6e3b9ccd16aaf4a25fb89e0ec13315d283fc7fc5c7e9885cbf8bbb2e1afeb27749a5afd92&scene=27#wechat_redirect


首先需要明白的零拷贝技术，然后零拷贝技术在java的实现时使用堆外内存操作

从一个channel里面读数据的过程 首先在创建一个堆内内存的时候 会在JVM外面创建一个内存 这个内存有操作系统负责 

读数据时 首先将数据读到操作系统分配的内存里面 然后复制到JVM到堆内存  明白为什么 操作系统不能直接把数据写到堆内内存
因为涉及到JVM的垃圾回收机制 垃圾回收机制会导致内存地址的变化 然后会导致os无法寻址  所以这个任务有JVM来实现 这个可见是个优化的空间



在Netty中零拷贝有哪些地方？
一共三个地方transferTo
1. Netty的接收和发送ByteBuffer采用DIRECT BUFFERS，使用堆外直接内存进行Socket读写，不需要进行字节缓冲区的二次拷贝。如果使用传统的堆内存（HEAP BUFFERS）进行Socket读写，JVM会将堆内存Buffer拷贝一份到直接内存中，然后才写入Socket中。相比于堆外直接内存，
消息在发送过程中多了一次缓冲区的内存拷贝
Netty 创建的 ByteBuffer 类型，由 ChannelConfig 配置。
而 ChannelConfig 配置的 ByteBufAllocator 默认创建 Direct Buffer 类型

2.CompositeByteBuf类
可以将多个 ByteBuf 合并为一个逻辑上的 ByteBuf ，避免了传统通过内存拷贝的方式将几个小 Buffer 合并成一个大的 Buffer 
addComponents(...) 方法，可将 header 与 body 合并为一个逻辑上的 ByteBuf 。这两个 ByteBuf 在CompositeByteBuf 内部都是单独存在的，
即 CompositeByteBuf 只是逻辑上是一个整体

3.通过 FileRegion 包装的 FileChannel 。
 tranferTo(...) 方法，实现文件传输, 可以直接将文件缓冲区的数据发送到目标 Channel ，避免了传统通过循环 write 方式，导致的内存拷贝问题
 
 ```java
 long transferTo(WritableByteChannel target, long position) throws IOException;
```

4.通过 wrap 方法, 我们可以将 byte[] 数组、ByteBuf、ByteBuffer 等包装成一个 Netty ByteBuf 对象, 进而避免了拷贝操作

