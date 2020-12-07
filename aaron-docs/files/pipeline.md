1.netty 如何判断ChannelHandler类型？？

2.ChannelInitializer 
ChannelInitializer的主要目的是为程序员提供了一个简单的工具，用于在某个Channel注册到EventLoop后，对这个Channel执行一些初始化操作。ChannelInitializer虽然会在一开始会被注册到Channel相关的pipeline里，但是在初始化完成之后，ChannelInitializer会将自己从pipeline中移除，不会影响后续的操作。
使用场景：
a. 在ServerBootstrap初始化时，为监听端口accept事件的Channel添加ServerBootstrapAcceptor
b. 在有新链接进入时，为监听客户端read/write事件的Channel添加用户自定义的ChannelHandler