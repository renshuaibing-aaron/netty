package io.netty.example.aaron.zerocopy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 NIO 的 sendFile 服务端.
 *
 * @author cxs
 */
public class NioSendFileServer implements SendFileServer {

    private static Logger logger = LoggerFactory.getLogger(NioSendFileServer.class);

    /**
     * 4 个读线程, 每个线程管理一个 selector, 每个 selector 管理多个 socketChannel.
     */
    private KernelReadProcessor[] readWorkers = new KernelReadProcessor[4];
    private int muskReadWorkersLength = 0;
    private int nextCursor = 0;
    /**
     * 4 个线程, 用于回写数据给客户端.
     */
    private KernelWriteProcessor[] write_workers = new KernelWriteProcessor[4];
    private KernelAcceptProcessor acceptProcessor;
    private AtomicBoolean running = new AtomicBoolean();
    private ServerSocketChannel serverSocketChannel;

    private KernelReadProcessor nextKernelReadProcessor() {
        return readWorkers[Math.abs(++nextCursor) & muskReadWorkersLength];
    }


    @Override
    public void start(String address, int port, String baseDir) throws IOException {
        try {
            if (!running.compareAndSet(false, true)) {
                throw new RuntimeException("send file httpserver already running.");
            }

            for (int i = 0; i < readWorkers.length; i++) {
                readWorkers[i] = new KernelReadProcessor(baseDir, Selector.open());
                readWorkers[i].start();
            }

            muskReadWorkersLength = readWorkers.length - 1;

            for (int i = 0; i < write_workers.length; i++) {
                write_workers[i] = new KernelWriteProcessor();
                write_workers[i].start();
            }

            doStart(address, port);
        } catch (Exception e) {
            running.set(false);
            throw e;
        }
    }


    private void doStart(String address, int port) throws IOException {
        SelectionKey key = null;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(address, port));
        // 此 selector 专门用于 accept 连接.
        Selector acceptSelector = Selector.open();

        acceptProcessor = new KernelAcceptProcessor(acceptSelector);
        // 把 httpserver socket 和 accept 注册到 这个 selector 中.
        acceptProcessor.register(serverSocketChannel);

        logger.info("send file httpserver start success and ready accept, httpserver info = {}", serverSocketChannel.socket());

        while (running.get()) {
            try {
                acceptSelector.select();
                Iterator<SelectionKey> iterator = acceptSelector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    // must remove. cpu 100%
                    iterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);

                        nextKernelReadProcessor().register(socketChannel);

                        logger.info("accept a new socket {}", socketChannel.socket());
                    }
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                if (key != null) {
                    try {
                        key.channel().close();
                    } catch (IOException ex) {
                        logger.warn(ex.getMessage(), ex);
                    }
                }
            }
        }
    }

    @Override
    public void shutdown() {
        for (int i = 0; i < readWorkers.length; i++) {
            readWorkers[i].stop();
            write_workers[i].stop();
        }
        try {
            logger.info("send file httpserver stop success. httpserver socket = {}", serverSocketChannel.socket());
            serverSocketChannel.close();
        } catch (IOException e) {
            // ignore.
        }
        running.set(false);
    }
}
