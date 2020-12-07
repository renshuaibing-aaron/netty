1.解释为什么不需要进行监听写事件,因为水平触发要内核缓冲区还不满就会一直写就绪。
  所以只有需要写的时候 再进行监听
  内核缓存区  这里涉及到 水平触发还是边缘触发
https://www.jianshu.com/p/6bdee8cfee90
2.reactor 模型有三种 
  io.netty.example.aaron.nio  ---- 单Reactor 单线程模型
  
  其中客户端发送请求至服务端，Reactor响应其IO事件。
  如果是建立连接的请求，则将其分发至acceptor，由其接受连接，然后再将其注册至分发器。
  如果是读请求，则分发至Handler，由其读出请求内容，然后对内容解码，然后处理计算，再对响应编码，最后发送响应
  在整个过程中都是使用单线程，无论是Reactor线程和后续的Handler处理都只使用一个线程。
  
  但是单线程无疑会降低性能，所以需要增加线程提供扩展。
  
  问题:所有的I/O连接，读写都在一个线程，串行，效率太低  
  
  解决:读写操作在一个线程池里操作 
  io.netty.example.aaron.reactor.threadpool  ---- 单Reactor 多线程模型  
  
  问题：连接，读写一个线程，在百万级连接情况下，影响性能
  解决：连接和读写分开
  io.netty.example.aaron.reactor.masterslave  ---- 主从Reactor 多线程模型  
  
3.
https://www.cnblogs.com/lxyit/p/10430939.html