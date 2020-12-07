1.在讲netty的解码编码之前 说一说粘包和拆包问题
由于TCP协议自己的特性 在进行socket编程的时候 会出现粘包和拆包的问题 针对这个问题 netty有自己的解决方法


2.讲一讲ByteBuf

我们知道 在pipeline的处理事件的时候 最原始的代码是这样的  这个方法参数为什么可以强转成ByteBuf?这个是解决 粘包拆包的关键 只有确定了这个关键类 然后可以进行累加操作
来进行处理  因为我们知道Netty的本质是对java nio的封装  在java nio中出现的ByteBuffer 这里的ByteBuf是netty对java nio的封装 那么可以肯定的是
在netty中肯定出现了 由java nio的ByteBuffer到ByteBuf的转化    这个地方出现在AbstractNioByteChannel#NioByteUnsafe#read()方法里面
可以对比在java nio中的read方法(注意和serverSocketChannel的read的方法的对比,这个方法是进行读取连接用的)  在这个read方法里面进行了上述的转化，并且
在这个方法里面可以看出netty关于ByteBuf的骚操作  
  ```java
 public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf=(ByteBuf) msg;
 }
```