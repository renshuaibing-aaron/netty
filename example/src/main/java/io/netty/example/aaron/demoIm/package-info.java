package io.netty.example.aaron.demoIm;

/*
* inbound事件：
inbound事件的传播#
什么是inbound事件#
inbound事件其实就是客户端主动发起事件,比如说客户端请求连接,连接后客户端有主动的给服务端发送需要处理的有效数据等,只要是客户端主动发起的事件,都算是Inbound事件,特征就是事件触发类型,当channel处于某个节点,触发服务端传播哪些动作

netty如何对待inbound#
netty为了更好的处理channel中的数据,给jdk原生的channel添加了pipeline组件,netty会把原生jdk的channel中的数据导向这个pipeline,从pipeline中的header开始 往下传播, 用户对这个过程拥有百分百的控制权,可以把数据拿出来处理, 也可以往下传播,一直传播到tail节点,tail节点会进行回收,如果在传播的过程中,最终没到尾节点,自己也没回收,就会面临内存泄露的问题

一句话总结,面对Inbound的数据, 被动传播
*
*
*
* 什么是outBound事件#
创建的outbound事件如: connect,disconnect,bind,write,flush,read,close,register,deregister, outbound类型事件更多的是服务端主动发起的事件,
* 如给主动channel绑定上端口,主动往channel写数据,主动关闭用户的的连接
*
* */