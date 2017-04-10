package com.nxttxn.vramel.impl.jpos;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.net.NetClient;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/31/13
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class JPOSClient {
    public static final String DEFAULT_KEY = "1000";
    protected final Logger logger = LoggerFactory.getLogger(JPOSClient.class);

    private static final int DELAY = 200;
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int ALWAYS_ATTEMPT_RECONNECT = -1;
    private static final String networkManagementRequestMTI = "2800";
    public static final String networkManagementResponseMTI = "2810";
    private boolean active;
    private final NetClient netClient;
    private final Vertx vertx;
    private final URI uri;
    private JPOSChannel jposChannel;
    private final String name;

    private final String[] keyFields;

    public JPOSClient(Vertx vertx, URI uri, String keyFields) {
        this(vertx, uri, keyFields, -1);
    }

    public JPOSClient(Vertx vertx, URI uri, String keyFields, int connectTimeout) {
        this.vertx = vertx;
        this.uri = uri;
        this.keyFields = keyFields.split(",");
        this.name = uri.getHost()+":"+uri.getPort();

        netClient = vertx.createNetClient().setReconnectAttempts(ALWAYS_ATTEMPT_RECONNECT);
        if (connectTimeout > 0) {
            netClient.setConnectTimeout(connectTimeout);
        }
        establishConnection();
    }

    private ConcurrentMap<String, byte[]> getIsoMsgResults() {
        return vertx.sharedData().getMap("ISOMsgResults");
    }


    private Handler<ISOMsg> isoMsgReplyHandler = new Handler<ISOMsg>() {
        @Override
        public void handle(ISOMsg isoMsg) {
            try {
                final Map<String, byte[]> isoMsgResults = getIsoMsgResults();
                String key = getKey(isoMsg);
                isoMsgResults.put(key, isoMsg.pack());
            } catch (ISOException e) {
                logger.error("[JPOSClient"+name+"] Problem handling a response from JPOS.", e);
            }
        }
    };

    private String getKey(ISOMsg isoMsg) {
        StringBuilder sb = new StringBuilder();
        for (String fieldId : keyFields) {
            sb.append(isoMsg.getString(fieldId));
        }
        return sb.toString();
    }

    private void establishConnection() {
        final String host = uri.getHost();
        final Number port = uri.getPort();

        jposChannel = new JPOSChannel("c<"+name+">");
        jposChannel.isoMsgHandler(isoMsgReplyHandler);
        jposChannel.connectedHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                try {
                    logger.info("[JPOSClient-"+name+"] Verifying connection with JPOS");
                    initializeJPOSChannel(new AsyncResultHandler<ISOMsg>() {
                        @Override
                        public void handle(AsyncResult<ISOMsg> isoMsgAsyncResult) {
                            if (isoMsgAsyncResult.failed()) {
                                logger.error("[JPOSClient-"+name+"] Unable to establish connection with JPOS within timeout.", isoMsgAsyncResult.exception);
                                return;
                            }

                            active = true;
                            logger.info("[JPOSClient-"+name+"] JPOS connection established: {}", isoMsgAsyncResult.result);
                        }
                    });
                } catch (Exception e) {
                    logger.error("[JPOSClient-"+name+"] cannot verify JPOS connection", e);
                }
            }
        });
        jposChannel.disconnectedHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                active = false;
                logger.info("[JPOSClient-"+name+"] JPOSChannel disconnected. Attempting to reestablish connection.");
                establishConnection();
            }
        });

        netClient.exceptionHandler(new Handler<Exception>() {
            @Override
            public void handle(Exception e) {
                logger.error("[JPOSClient-"+name+"] NetClient connection exception", e);
            }
        });
        netClient.connect(port.intValue(), host, jposChannel);
    }


    public boolean isActive() {
        return active;
    }


    public void sendISOMsg(final ISOMsg isoMsg, final AsyncResultHandler<ISOMsg> asyncResultHandler) {
        sendISOMsg(isoMsg, asyncResultHandler, DEFAULT_TIMEOUT);
    }

    public void sendISOMsg(final ISOMsg isoMsg, final AsyncResultHandler<ISOMsg> asyncResultHandler, int timeout) {
        try {
            final String key = getKey(isoMsg);

            //first make sure it's not already waiting for us
            ISOMsg result = removeResult(key);
            if (result != null) {
                asyncResultHandler.handle(new AsyncResult<ISOMsg>(result));
                return;
            }

            jposChannel.sendISOMsg(isoMsg);
            receiveISOMsg(key, asyncResultHandler, timeout);
        } catch (Exception e) {
            asyncResultHandler.handle(new AsyncResult<ISOMsg>(e));
        }
    }

    public ISOMsg removeResult(String key) throws ISOException {
        final byte[] response = getIsoMsgResults().remove(key);
        if (response == null) {
            return null;
        }
        ISOMsg result = JPOSChannelIn.buildISOMsgFromBytes(response);
        return result;
    }

    public void receiveISOMsg(String key, AsyncResultHandler<ISOMsg> asyncResultHandler, int timeout) {
        final int iterationsToCheck = timeout == -1 ? -1 : timeout / DELAY;
        vertx.setPeriodic(DELAY, new ISOMsgReceiver(asyncResultHandler, iterationsToCheck, vertx, key));

    }

    private void initializeJPOSChannel(AsyncResultHandler<ISOMsg> asyncResultHandler) throws Exception {
        final int neverTimeout = -1;
        sendISOMsg(buildNetworkManagementRequestMsg(), asyncResultHandler, neverTimeout);
    }


    private ISOMsg buildNetworkManagementRequestMsg() throws Exception {
        ISOMsg netReq = new ISOMsg();
        netReq.setMTI(networkManagementRequestMTI);
        for (String fieldId : keyFields) {
            netReq.set(fieldId, UUID.randomUUID().toString());
        }

        return netReq;
    }

    public void whenActive(int timeout, AsyncResultHandler<Void> asyncResultHandler) {
        if (active) {
            asyncResultHandler.handle(new AsyncResult<>((Void) null));
            return;
        }

        logger.info("[JPOSClient-"+name+"] not yet active. Will wait for {} ms", timeout);
        vertx.setPeriodic(DELAY, new AbstractTimeoutHandler<Void>(asyncResultHandler, timeout / DELAY, vertx) {
            @Override
            protected boolean resultReceived() {
                if (active) {
                    logger.info("[JPOSClient-"+name+"] is now active. Notifying whenActive caller.");
                    asyncResultHandler.handle(new AsyncResult<Void>((Void) null));
                }
                return active;
            }
        });
    }


    private class ISOMsgReceiver extends AbstractTimeoutHandler<ISOMsg> {
        private final String key;

        public ISOMsgReceiver(AsyncResultHandler<ISOMsg> asyncResultHandler, int iterationsToCheck, Vertx vertx, String key) {
            super(asyncResultHandler, iterationsToCheck, vertx);
            this.key = key;
        }

        protected boolean resultReceived() {
            try {
                final ISOMsg resultIsoMsg = removeResult(key);
                if (resultIsoMsg != null) {
                    asyncResultHandler.handle(new AsyncResult<>(resultIsoMsg));
                    return true;
                }
            } catch (ISOException e) {
                logger.warn("[JPOSClient-"+name+"] Error reading ISO Msg result. Will keep trying until timeout.", e);
            }
            return false;
        }
    }
}
