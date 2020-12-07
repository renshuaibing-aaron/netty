package io.netty.example.aaron.takelongtime.customize;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.*;

@ChannelHandler.Sharable
public class EchoServerHandler extends SimpleChannelInboundHandler<String> {
    public static final ChannelHandler INSTANCE = new EchoServerHandler();
    private static final String LINE = System.getProperty("line.separator");

    private EchoServerHandler() {
    }
    protected static ExecutorService newFixedThreadPool() {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("netty-business-%d")
                .setDaemon(false)
                .build();
        return new ThreadPoolExecutor(200, 200,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(10000),
                threadFactory);
    }
    final static ListeningExecutorService service = MoreExecutors.listeningDecorator(newFixedThreadPool());
    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) {
        service.submit(new Runnable() {
            @Override
            public void run() {
                //模拟耗时任务
                try {
                    System.out.println("=================="+msg);
                    Thread.sleep(1000);
                    System.out.println("execute time 5s");
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                ctx.writeAndFlush(msg + LINE).addListener(ChannelFutureListener.CLOSE);
            }
        });

    }
}
