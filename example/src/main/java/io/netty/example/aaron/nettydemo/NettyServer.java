package io.netty.example.aaron.nettydemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * descripiton:服务端
 */
public class NettyServer {
    /**
     * 端口
     */
    private int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void run() {
        //EventLoopGroup是用来处理IO操作的多线程事件循环器
        //负责接收客户端连接线程
        System.out.println("=====================");
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);


        //负责处理客户端i/o事件、task任务、监听任务组
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);


        //启动 NIO 服务的辅助启动类
        ServerBootstrap bootstrap = new ServerBootstrap();


        //一个是group，一个是channelFactory，在这里可以想一想这两个参数是在哪里以及何时被赋值的？
        // 其中是将bossGroup赋值给了group,将BootstrapChannelFactory赋值给了channelFactory.
        //2.设置group：ServerBootstrap继承AbstractBootstrap，
        // bossgroup是设置AbstractBootstrap的group属性，work是设置ServerBootstrap的childGroup属性
        bootstrap.group(bossGroup, workerGroup);



        //配置 Channel
        ServerBootstrap channel1 = bootstrap.channel(NioServerSocketChannel.class);

        //3.设置channel：未来channel的属性设置就是根据我们传入的class,NioServerSocketChannel只实现了bind相关方法，connect没有实现，所以NioServerSocketChannel只适合服务端
        // 4.设置loaclAddress:设置localAddress
        //5.childHandler和handler：handler 字段与 accept 过程有关, 即这个 handler 负责处理客户端的连接请求; 而 childHandler 就是负责和客户端的连接的 IO 交互.
        //6.option和childoption：option 字段与 accept 过程有关, 即这个 handler 负责处理客户端的连接请求; 而 childoption 就是负责和客户端的连接的 IO 交互.
        bootstrap.handler(new SimpleServerHandler());

        bootstrap.childHandler(new ServerIniterHandler());

        //BACKLOG用于构造服务端套接字ServerSocket对象，
        // 标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        //是否启用心跳保活机制
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            //绑定服务端口监听
            Channel channel = bootstrap.bind(port).sync().channel();
            System.out.println("httpserver run in port " + port);
            //服务器关闭监听
            /*channel.closeFuture().sync()实际是如何工作:
            channel.closeFuture()不做任何操作，只是简单的返回channel对象中的closeFuture对象，对于每个Channel对象，都会有唯一的一个CloseFuture，用来表示关闭的Future，
            所有执行channel.closeFuture().sync()就是执行的CloseFuturn的sync方法，从上面的解释可以知道，这步是会将当前线程阻塞在CloseFuture上*/
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭事件流组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new NettyServer(8899).run();
    }
}
