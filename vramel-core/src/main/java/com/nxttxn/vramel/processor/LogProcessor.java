package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import org.slf4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 10:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogProcessor implements Processor {
    private final String message;
    private final Logger logger;

    public LogProcessor(String message, Logger logger) {
        this.message = message;
        this.logger = logger;
    }

    public void process(Exchange exchange) throws Exception {
        logger.info(String.format("%s: %s", message, exchange.toString()));
    }

}
