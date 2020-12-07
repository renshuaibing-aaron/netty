package io.netty.example.aaron.reactor.threadpool;

import io.netty.example.aaron.reactor.single.Handler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

public class AsyncHandler implements Handler {

    private final Selector selector;

    private final SelectionKey selectionKey;
    private final SocketChannel socketChannel;

    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer sendBuffer = ByteBuffer.allocate(2048);

    private final static int READ = 0; //读取就绪
    private final static int SEND = 1; //响应就绪
    private final static int PROCESSING = 2; //处理中

    private int status = READ; //所有连接完成后都是从一个读取动作开始的

    //开启线程数为5的异步处理线程池  注意这个地方是静态变量 所有的公用线程池
    private static final ExecutorService workers = new ThreadPoolExecutor(5, 200, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1), new DefaultThreadFactory("work thread"),new ThreadPoolExecutor.AbortPolicy()
    );


    AsyncHandler(SocketChannel socketChannel, Selector selector) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        selectionKey = socketChannel.register(selector, 0);
        selectionKey.attach(this);
        selectionKey.interestOps(SelectionKey.OP_READ);
        this.selector = selector;
        this.selector.wakeup();
    }

    //如果一个任务正在异步处理，那么这个run是直接不触发任何处理的，
    // read和send只负责简单的数据读取和响应，业务处理完全不阻塞这里的处理
    @Override
    public void handle() {
        System.out.println("======AsyncHandler========="+Thread.currentThread());

        switch (status) {
            case READ:
                read();
                break;
            case SEND:
                send();
                break;
            default:
        }
    }

    private void read() {
        if (selectionKey.isValid()) {
            try {
                readBuffer.clear();
                int count = socketChannel.read(readBuffer);
                if (count > 0) {
                    status = PROCESSING; //置为处理中，处理完成后该状态为响应，表示读入处理完成，接下来可以响应客户端了
                    workers.execute(this::readWorker); //异步处理
                } else {
                    selectionKey.cancel();
                    socketChannel.close();
                    System.out.println("read时-------连接关闭");
                }
            } catch (IOException e) {
                System.err.println("处理read业务时发生异常！异常信息：" + e.getMessage());
                selectionKey.cancel();
                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    System.err.println("处理read业务关闭通道时发生异常！异常信息：" + e.getMessage());
                }
            }
        }
    }

    void send() {
        if (selectionKey.isValid()) {
            status = PROCESSING; //置为执行中
            workers.execute(this::sendWorker); //异步处理
            selectionKey.interestOps(SelectionKey.OP_READ); //重新设置为读
        }
    }

    //读入信息后的业务处理
    private void readWorker() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(String.format("收到来自客户端的消息: %s",
                new String(readBuffer.array())));
        status = SEND;
        selectionKey.interestOps(SelectionKey.OP_WRITE); //注册写事件
        this.selector.wakeup(); //唤醒阻塞在select的线程，因为该interestOps写事件是放到子线程的，select在该channel还是对read事件感兴趣时又被调用，因此如果不主动唤醒，select可能并不会立刻select该读就绪事件（在该例中，可能永远不会被select到）
    }

    private void sendWorker() {
        try {
            sendBuffer.clear();
            sendBuffer.put(String.format("我收到来自%s的信息辣：%s,  200ok;",
                    socketChannel.getRemoteAddress(),
                    new String(readBuffer.array())).getBytes());
            sendBuffer.flip();

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int count = socketChannel.write(sendBuffer);

            if (count < 0) {
                selectionKey.cancel();
                socketChannel.close();
                System.out.println("send时-------连接关闭");
            } else {
                //再次切换到读
                status = READ;
            }
        } catch (IOException e) {
            System.err.println("异步处理send业务时发生异常！异常信息：" + e.getMessage());
            selectionKey.cancel();
            try {
                socketChannel.close();
            } catch (IOException e1) {
                System.err.println("异步处理send业务关闭通道时发生异常！异常信息：" + e.getMessage());
            }
        }
    }
}