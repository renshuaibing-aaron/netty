1.解释为什么不需要进行监听写事件,因为水平触发要内核缓冲区还不满就会一直写就绪。
https://www.jianshu.com/p/6bdee8cfee90
2.reactor 模型有三种 
  io.netty.example.aaron.singlereactor  ---- 单Reactor 单线程模型
  
  问题:所有的I/O连接，读写都在一个线程，串行，效率太低  
  
  解决:读写操作在一个线程池里操作 
  io.netty.example.aaron.reactor.threadpool  ---- 单Reactor 多线程模型  
  
  问题：连接，读写一个线程，在百万级连接情况下，影响性能
  解决：连接和读写分开
  io.netty.example.aaron.reactor.masterslave  ---- 主从Reactor 多线程模型  