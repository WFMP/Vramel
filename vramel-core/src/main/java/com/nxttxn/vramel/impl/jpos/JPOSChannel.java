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
        //Setup Pump using In and Out channels
        final Pump outPump = Pump.createPump(out, socket);
        final Pump inPump = Pump.createPump(socket, in);

        socket.exceptionHandler(new Handler<Exception>() {
            @Override
            public void handle(Exception e) {
                logger.error(logPrefix+"socket exception", e);
                outPump.stop();
                inPump.stop();
                if (disconnectedHandler != null) {
                    disconnectedHandler.handle(null);
                }
            }
        });
        socket.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                logger.info(logPrefix+"Socket shutting down. Deactivating JPOSChannel.");
                outPump.stop();
                inPump.stop();
            }
        });

        socket.closedHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                logger.info(logPrefix +"Socket closed.");
                outPump.stop();
                inPump.stop();
                if (disconnectedHandler != null) {
                    disconnectedHandler.handle(null);
                }
            }
        });

        outPump.start();
        inPump.start();

        if (connectedHandler != null) {
            connectedHandler.handle(null);
        }
    }


    void connectedHandler(Handler<Void> connectedHandler) {

        this.connectedHandler = connectedHandler;
    }

    void disconnectedHandler(Handler<Void> disconnectedHandler) {

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
