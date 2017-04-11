package com.nxttxn.vramel.impl.jpos;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.testframework.TestBase;
import org.vertx.java.testframework.TestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Test JPOSClient
 */
public class JPOSClientTest extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(JPOSClientTest.class);
    private TestUtils tu = new TestUtils(vertx);
    private static final int port = 9183;
    private static final String host = "127.0.0.1";
    private final URI uri = uri();

    /**
     * Test that JPOSClient remains inactive if it cannot connect to a JPOS server and than whenActive times out
     * as expected.
     */
    public void testWhenActiveTimeoutWhenNoServerListening() throws Exception {
        JPOSClient jposClient = createJPOSClient();

        tu.azzert(!jposClient.isActive(), "JPOSClient connected with no server running");

        jposClient.whenActive(600, new AsyncResultHandler<Void>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                try {
                    log.info("JPOSClient.whenActive() " + (event.failed() ? "failed" : "active"));
                    if (event.failed()) {
                        log.info("exception:", event.exception);
                    }
                    tu.azzert(event.failed(), "whenActive failed");
                    tu.azzert(event.failed() && (event.exception instanceof  JPOSResponseTimeoutException), "Exception is not a Timeout");
                } finally {
                    log.info("Completed test: {}", getName());
                    tu.testComplete();
                }
            }
        });

        startTest(getName(), 1);
    }

    /**
     * Test that JPOSClient state changes from in-active to active once it connects to a JPOS server and completes the
     * 2800 ACK handshake
     */
    public void testBecomesActiveAfterServerStarts() throws Exception {
        // Create a client and start trying to connect...
        final JPOSClient jposClient = createJPOSClient();

        // Assert it is still inactive
        tu.azzert(!jposClient.isActive());

        // Wait 500ms and then assert it is still inactive, then start a server up.
        vertx.setTimer(500, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                boolean testFailed = true;
                try {
                    tu.azzert(!jposClient.isActive());
                    createJPOSServer();
                    testFailed = false;
                } finally {
                    if (testFailed) {
                        completeTest();
                    }
                }
            }
        });


        // Wait for the client to become active. Assert that the client becomes active without timing out.
        jposClient.whenActive(30000, new AsyncResultHandler<Void>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                try {
                    tu.azzert(!event.failed());
                    tu.azzert(jposClient.isActive());
                } finally {
                    completeTest();
                }
            }
        });

        // Wait for the test to complete
        startTest();
    }

    /**
     * Test that the JPOSClient recovers when the server closes the connection while waiting for the server's
     * response to the 2800 handshake message.
     */
    public void testRecoverFromServerClosingConnectionDuringHandshake() throws Exception {
        // Start a server that closes the connection when it gets a 2800.
        final JPOSServer jposServer = createDisconnectOnAckServer();

        // Start a client...
        final JPOSClient jposClient = createJPOSClient();

        // Confirm the client is not active
        tu.azzert(!jposClient.isActive());

        // Wait 800ms then start a server that will response to ACK
        vertx.setTimer(800, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                boolean testFailed = true;
                JPOSServer jposServer2 = null;

                try {
                    tu.azzert(!jposClient.isActive());
                    jposServer2 = createJPOSServer();
                    // Wait for the client to become active
                    final JPOSServer finalJposServer = jposServer2;
                    jposClient.whenActive(2500, new AsyncResultHandler<Void>() {
                        @Override
                        public void handle(AsyncResult<Void> event) {
                            try {
                                tu.azzert(!event.failed(), "whenActive failed. Reason: " + (event.exception == null ? "" : event.exception.getMessage()));
                                tu.azzert(jposClient.isActive(), "JPOSClient is not active. Expected it to be active");
                            } finally {
                                completeTest();
                                finalJposServer.close();
                            }
                        }
                    });
                    testFailed = false;
                } finally {
                    // azzert could throw  an exception. If that happens we failed and should quite the test
                    if (testFailed) {
                        log.info("Failure in server start timer");
                        completeTest();
                        jposServer2.close();
                    }
                }
            }
        });


        startTest();
    }

    /**
     * Test that the client recovers if the server closes the connection immediately after the client connects.
     *
     * The server will accept the client connection and then close the connection and stop listening, before binding
     * the listen socket again and accepting new connections.
     */
    public void testRecoverFromConnectionResetByPeerImmediatelyAfterClientConnect() throws Exception {
        // Start a server that closes the connection when it gets a 2800.
        final JPOSServer jposServer = createDisconnectOnAckServer();

        vertx.setTimer(180, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                closeServer(jposServer);
            }
        });

        // Start a client...
        final JPOSClient jposClient = createJPOSClient();

        // Confirm the client is not active
        tu.azzert(!jposClient.isActive());

        // Wait 800ms then start a server that will respond to ACK
        vertx.setTimer(800, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                boolean testFailed = true;
                try {
                    tu.azzert(!jposClient.isActive());
                    createJPOSServer();
                    // Wait for the client to become active
                    jposClient.whenActive(2500, new AsyncResultHandler<Void>() {
                        @Override
                        public void handle(AsyncResult<Void> event) {
                            try {
                                tu.azzert(!event.failed(), "whenActive failed. Reason: " + (event.exception == null ? "" : event.exception.getMessage()));
                                tu.azzert(jposClient.isActive(), "JPOSClient is not active. Expected it to be active");
                            } finally {
                                completeTest();
                            }
                        }
                    });
                    testFailed = false;
                } finally {
                    // azzert could throw  an exception. If that happens we failed and should quite the test
                    if (testFailed) {
                        log.info("Failure in server start timer");
                        completeTest();
                    }
                }
            }
        });


        startTest();
    }

    public void testRecoversFromConnectionResetByPeerOnActiveClient() throws Exception {
        // Start a server that closes the connection when it gets a 2800.
        final BadJPOSServer jposServer = createBadJPOSServer();

        // Start a client...
        final JPOSClient jposClient = createJPOSClient();

        // Confirm the client is not active
        tu.azzert(!jposClient.isActive());

        // Wait for the client to become active
        jposClient.whenActive(2500, new AsyncResultHandler<Void>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                boolean testFailed = false;
                try {
                    tu.azzert(!event.failed(), "whenActive failed. Reason: " + (event.exception == null ? "" : event.exception.getMessage()));
                    tu.azzert(jposClient.isActive(), "JPOSClient is not active. Expected it to be active");

                    vertx.setTimer(100, new Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            jposServer.closeClients();
                        }
                    });
//                    jposServer.close();
                    vertx.setTimer(500, new Handler<Long>() {
                        @Override
                        public void handle(Long event) {
                            log.info("JPOSClient active? {}", jposClient.isActive() ? "yes" : "false");
                            completeTest();
                            jposServer.close();
                        }
                    });
                } finally {
                    if(testFailed) {
                        completeTest();
                        jposServer.close();
                    }
                }
            }
        });

        startTest();

    }



    private void closeServer(JPOSServer jposServer) {
        log.info("Closing server side down");
        jposServer.close();
    }

    private ISOMsg makeAckMsg() throws ISOException {
        return makeISOMsg("2800");
    }

    private ISOMsg makeISOMsg(String mti) throws ISOException {
        ISOMsg netReq = new ISOMsg();
        netReq.setMTI(mti);
        netReq.set("1000", UUID.randomUUID().toString());

        return netReq;
    }

    private void startTest() {
        startTest(getName(), 30);
    }

    private void startTest(final String testName, int timeout) {
        log.info("Starting test: " + testName);
        tu.startTest(testName);
        waitTestComplete(timeout);
    }
    private void completeTest() {
        log.info("Completed test: {}", getName());
        tu.testComplete();
    }

    private BadJPOSServer createBadJPOSServer() {
        log.info("Create BadJPOSServer listening on {}:{}", host, port);
        BadJPOSServer server = new BadJPOSServer(vertx);
        return server.jposServerRequestHandler(createEchoHandler()).listen( port, host);
    }

    private JPOSServer createJPOSServer() {
        log.info("Create JPOSServer listening on {}:{}", host, port);
        JPOSServer server = new JPOSServer(vertx);
        return server.jposServerRequestHandler(createEchoHandler()).listen(port, host);
    }


    private JPOSServer createDisconnectOnAckServer() {
        log.info("Create JPOSServer that disconnects when it gets a 2800 (ACK) message");
        final JPOSServer jposServer =  new JPOSServer(vertx);
        return jposServer.jposServerRequestHandler(new TestJPOSServerRequestHandler() {
            @Override
            void handleRequest(JPOSServerRequest event) throws Exception {
                log.info("Got ISOMsg. MTI {}", event.getMTI());
                closeServer(jposServer);
            }
        }).listen(port, host);

    }

    private Handler<JPOSServerRequest> createEchoHandler() {
        log.info("Create JPOSServerRequest handler that echo's requests");
        return new TestJPOSServerRequestHandler() {
            @Override
            void handleRequest(JPOSServerRequest event) throws Exception {
                log.info("Got ISOMsg. MTI {}", event.getMTI());
                ISOMsg msg = event.getIsoMsg();
                msg.setResponseMTI();
                log.info("Sending response with MTI {}", msg.getMTI());
                event.getOut().sendISOMsg(msg);
            }
        };
    }

    private JPOSClient createJPOSClient() {
        log.info("Create JPOSClient connecting to {}", uri);
        return new JPOSClient(vertx, uri, JPOSClient.DEFAULT_KEY, 2);
    }

    private abstract class TestJPOSServerRequestHandler implements Handler<JPOSServerRequest> {

        @Override
        public void handle(JPOSServerRequest event) {
            try {
                handleRequest(event);
            } catch (Exception e) {
                log.error("Error handling JposServerRequest", e);
            }
        }

        abstract void handleRequest(JPOSServerRequest event) throws Exception;
    }

    private static URI uri() {
        try {
            return new URI("jpos://"+host+":"+port);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}