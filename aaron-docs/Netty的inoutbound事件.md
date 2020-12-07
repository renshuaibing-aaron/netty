1.说明常见的inbound事件和常见的outbound事件

ChannelInBoundHandler对从客户端发往服务器的报文进行处理，一般用来执行半包/粘包，解码，读取数据，业务处理等；


ChannelOutBoundHandler对从服务器发往客户端的报文进行处理，一般用来进行编码，发送报文到客户端。


2.彻底搞懂Inbound事件和OutBound事件 初次见到这个inbound和outbound事件时很容易搞糊涂 为什么ChannelOutboundHandler事件里面也有read()方法
如果outbound对应的是IO输出事件  为什么这个接口里面会有表示IO的read方法呢？
众所周知，netty是基于事件驱动的 事件分为两类 inbound事件和outbound事件 这里的in和out并非是指IO的输入输出 而是指事件的类型
根据事件的触发的源头 分为两类
Inbound事件 外部触发 外部程序触发的事件 比如socket上有数据读取进来(这里是读取完了，不是读取这个动作)  比如socket连接上来了并被注册到某个EventLoop上
channelActive / channelInactive
channelRead
channelReadComplete
channelRegistered / channelUnregistered
channelWritabilityChanged
exceptionCaught
userEventTriggered

OutBound事件  由应用程序主动请求触发的事件 可以认为 outbound是指应用程序发起的某个操作 比如向某个socket里面读取数据(主动发起读取这个动作，而非读完
这个事件)  还有绑定地址  建立和关闭连接 IO操作 deregister(netty自定义的一些操作用于解除channel和eventloop的绑定关系)
bind
close
connect
deregister
disconnect
flush
read
write


3.注意这个类 DefaultChannelPipeline  
一旦一个socket建立 会建立一个pipeline  这个line上会有一些hander处理器  
如下
head-->hander1-->hander2-->hander3-->tail

这里面head和tail是netty自带的  其他的hander需要用户自定义指定(比如一些解码编码,业务处理逻辑等等)  
这里需要明白的 head节点是即是inbound事件处理 又是outbound事件处理器   tail节点是inbound事件处理器

事件发生 便开始在pipeline上进行传递  inbound事件 从head开始到tail  寻找inbound事件处理器 为什么tail是inbound事件 处理器 因为inbound事件是
被动触发的事件 需要用tail进行收尾  比如接收到了消息 如果在中间没有处理 会导致内存溢出  用tail进行收尾

outbound事件 从tail上进行传递  找outbound事件 最后到达head节点  这时候 一般交给head的unsafe进行处理  一般是写出操作 写到socket上



































