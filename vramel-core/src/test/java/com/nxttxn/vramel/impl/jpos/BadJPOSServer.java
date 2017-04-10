package com.nxttxn.vramel.impl.jpos;

import com.nxttxn.vramel.components.jpos.DefaultJPOSServerRequest;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implements something that behaves like a JPOSServer however, it has a method that can be used to close the read
 * channel on all connections so as to trigger a connection reset by peer error on the client side.
 */
public class BadJPOSServer {
    private static final Logger logger = LoggerFactory.getLogger(BadJPOSServer.class);

    private final Vertx vertx;
    private Handler<JPOSServerRequest> jposServerRequestHandler;
    private String logPrefix;
    private ServerSocketChannel server;
    private Selector selector;
    private Thread workerThread;
    private JPOSChannelIn in;
    private JPOSChannelOut out;
    private boolean stopWorking = false;

    public BadJPOSServer(Vertx vertx) {
        this.vertx = vertx;
    }


    public Handler<JPOSServerRequest> jposServerRequestHandler() {
        return jposServerRequestHandler;
    }

    public BadJPOSServer jposServerRequestHandler(Handler<JPOSServerRequest> jposServerRequestHandler) {
        this.jposServerRequestHandler = jposServerRequestHandler;
        return this;
    }

    public BadJPOSServer listen(int port, String host) {
        checkNotNull(jposServerRequestHandler);

        String name = host + ":" + port;
        logPrefix = String.format("[BadJPOSServer-%s-%x] ", name, hashCode());

        in = new JPOSChannelIn("s<"+ name +">");
        out = new JPOSChannelOut("s<"+ name +">");

        in.newISOMsgHandler(new Handler<ISOMsg>() {
            @Override
            public void handle(ISOMsg isoMsg) {
                jposServerRequestHandler.handle(new DefaultJPOSServerRequest(out, isoMsg));
            }
        });

        final ArrayDeque<ByteBuffer> writeBuffers = new ArrayDeque<>();

        out.dataHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                writeBuffers.addLast(ByteBuffer.wrap(event.copy().getBytes()));
            }
        });

        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(host, port));
            logger.info(logPrefix+"Server listening", name);
            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            logger.error(logPrefix+"Error creating server socket channel", e);
            return null;
        }

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {
                        selector.select();
                        if (stopWorking) {
                            logger.info(logPrefix+"Worker thread is stopping...");
                            break;
                        }
                        Set<SelectionKey> keys = selector.selectedKeys();
                        Iterator<SelectionKey> i = keys.iterator();

                        while(i.hasNext()) {
                            SelectionKey key = i.next();
                            i.remove();
                            if(key.isAcceptable()) {
                                acceptConnection();
                                continue;
                            }
                            if(key.isReadable()) {
                                readData((SocketChannel) key.channel());
                            }
                            if(key.isWritable()) {
                                writeData((SocketChannel) key.channel());
                            }
                        }
                    } catch (IOException e) {
                        logger.error(logPrefix+"IOError in worker thread", e);
                    }
                }
            }

            private void writeData(SocketChannel client) throws IOException {
                SocketAddress address = client.getRemoteAddress();
                while(writeBuffers.size() > 0) {
                    ByteBuffer b = writeBuffers.removeFirst();
                    int bytesWritten = client.write(b);
                    logger.info(logPrefix+"Writing {} of {} bytes to {}", bytesWritten, b.limit(), address);
                    if (b.hasRemaining() && bytesWritten == 0) {
                        b.compact();
                        writeBuffers.addFirst(b);
                        break;
                    }
                    if (bytesWritten <= 0)
                        break;
                }
            }

            private void readData(SocketChannel client) throws IOException {
                SocketAddress address = client.getRemoteAddress();
                logger.info(logPrefix+"{} as data to read", address );
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                while(true) {
                    int bytesRead = client.read(buffer);
                    if (bytesRead > 0 ) {
                        logger.info(logPrefix+"Read {} bytes from {}", bytesRead, address);
                        buffer.flip();
                        Buffer b = new Buffer(buffer.remaining());
                        b.setBytes(0,buffer);
                        in.writeBuffer(b);
                        buffer.compact();
                    } else {
                        logger.info(logPrefix+"{} has {} bytes to read", address, bytesRead);
                        break;
                    }
                }
            }

            private void acceptConnection() throws IOException {
                SocketChannel client = server.accept();
                client.configureBlocking(false);
                client.setOption(StandardSocketOptions.SO_LINGER, 0);
                client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE );
                logger.info("Accepted connect from {}", client.getRemoteAddress());
            }
        });

        stopWorking = false;

        workerThread.start();
        return this;
    }

    public void closeClients() {
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel) {
                SocketChannel client = (SocketChannel) key.channel();
                try {
                    logger.info(logPrefix+"shutting down input from client at {}", client.getRemoteAddress());
                    client.close();
                    key.cancel();
                } catch (IOException e) {
                    logger.error(logPrefix + "IOError shutting down server input", e);
                }
            }
        }
    }
    public void close() {
        try {
            stopWorking = true;
            selector.close();
            server.close();
        } catch (IOException e) {
            logger.error(logPrefix+"IO Error Closing BadJPOSServer", e);
        }
    }

}
