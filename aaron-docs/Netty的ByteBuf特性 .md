1.在Netty中，每个被申请的Buffer对于Netty来说都可能是很宝贵的资源，因此为了获得对于内存的申请与回收更多的控制权，
Netty自己根据引用计数法去实现了内存的管理。
  Netty对于Buffer的使用都是基于直接内存（DirectBuffer）实现的，大大提高I/O操作的效率
  然而DirectBuffer和HeapBuffer相比之下除了I/O操作效率高之外还有一个天生的缺点，
  即对于DirectBuffer的申请相比HeapBuffer效率更低
  因此Netty结合引用计数实现了PolledBuffer，即池化的用法，当引用计数等于0的时候，Netty将Buffer回收致池中，
  在下一次申请Buffer的没某个时刻会被复用。
  
  
  
2.可以用户自定义的缓冲区类型进行扩展  
  通过内置的符合缓冲区类型实现了透明的拷贝
  容量可以按需增长
  在读和写这两种模式之间切换不需要调用#flip()方法
  读写使用了不同的索引
  支持方法的链式调用
  支持引用计数
  支持池化技术

































  