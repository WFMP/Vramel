package com.nxttxn.vramel.impl.jpos;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;

/**
*
*/
public class JPOSChannel implements Handler<NetSocket> {
    protected final Logger logger = LoggerFactory.getLogger(JPOSChannel.class);
    private final JPOSChannelIn in;
    private final JPOSChannelOut out;
    private final String logPrefix;
    private Handler<Void> connectedHandler;
    private Handler<Void> disconnectedHandler;

    JPOSChannel(String name) {
        this(name, new JPOSChannelIn(name), new JPOSChannelOut(name));
    }

    JPOSChannel(String name, JPOSChannelIn in, JPOSChannelOut out) {
        this.logPrefix = String.format("[JPOSChannel-%s-%x] ",name, this.hashCode());
        this.in = in;
        this.out = out;
    }

    @Override
    public void handle(final NetSocket socket) {

        socket.exceptionHandler(new Handler<Exception>() {
            @Override
            public void handle(Exception e) {
                logger.error("[JPOSChannel] socket exception", e);
            }
        });
        socket.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                logger.info("[JPOSChannel] Socket shutting down. Deactivating JPOSChannel.");
            }
        });

        socket.closedHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                logger.info("[JPOSChannel] Socket closed.");
                if (disconnectedHandler != null) {
                    disconnectedHandler.handle(null);
                }
            }
        });

        //Setup Pump using In and Out channels
        Pump.createPump(out, socket).start();
        Pump.createPump(socket, in).start();

        if (connectedHandler != null) {
            connectedHandler.handle(null);
        }
    }


    public void connectedHandler(Handler<Void> connectedHandler) {

        this.connectedHandler = connectedHandler;
    }

    public void disconnectedHandler(Handler<Void> disconnectedHandler) {

        this.disconnectedHandler = disconnectedHandler;
    }

    JPOSChannel isoMsgHandler(Handler<ISOMsg> handler) {
        in.newISOMsgHandler(handler);
        return this;
    }

    void sendISOMsg(ISOMsg isoMsg) throws Exception {
        out.sendISOMsg(isoMsg);
    }
}
