1.说明Netty的本质就是Reactor模式的实现，Selector作为多路复用器，Eventloop 作为转发器，Pipeline作为事件处理器
但是作为一款优秀的框架，肯定会各种丰富，和一般的reactor模式不同的是 netty使用串行化的方式 并在pipeline中实现了责任链模式

netty中又对buffer又做了一些优化 
 怎么理解reactor模式  进一步思考 准确性啊
 
 reactor的三种模式 
 单reactor 单线程 所有的连接请求和读写在一个selector和线程中
 
 单reactor 线程池 连接请求和读写用一个selector 读写处理 用线程池处理  注意此时的线程模型是 1+n  1是指selector检测通道 包括连接和读写
                 有事件到来的时候 进行线程池处理 这时候 事件可以都放到线程池处理 或者部门 在这个下面的例子中是这样的 连接还是和selector共用线程
                 读写则用线程池处理
 
 主从reactor 线程池  其实就是利用多个selector(也就是reactor) 一个用来监听连接 一部门用来监听读写  这时候读写请求和连接请求具体用什么线程模型 自己决定
 
 
 其实selector就是个监听器 一旦有事件发生 然后取出事件 取下来上面的附属产品 然后就可以尽情处理了
 


