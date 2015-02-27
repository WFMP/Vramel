package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.impl.ProducerCache;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;


/**
 * Processor for forwarding exchanges to an endpoint destination.
 *
 * @version
 */
public class SendProcessor extends ServiceSupport implements AsyncProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SendProcessor.class);
    private final Endpoint endpoint;
    private ProducerCache producerCache;

    public SendProcessor(Endpoint endpoint) {
        this.endpoint = endpoint;
    }


    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    @Override
    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final Producer producer = getProducerCache().acquireProducer(endpoint);
        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(producer);
        ap.process(exchange, optionalAsyncResultHandler);
        return false;
    }

    protected void doStart() throws Exception {
        LOG.debug("Starting {}", this);
        getProducerCache();
        // Start the endpoint (incase it is not already started)
        ServiceHelper.startService(endpoint);
        producerCache.acquireProducer(endpoint);
    }

    protected void doStop() throws Exception {
        LOG.debug("Stopping {}", this);
        ServiceHelper.stopService(producerCache);
    }

    private ProducerCache getProducerCache() throws Exception {
        if (producerCache == null) {
            LOG.debug("Instantiating SendProcessor.producerCache for endpoint: {}", endpoint);
            // use a single producer cache as we need to only hold reference for one destination
            // and use a regular HashMap as we do not want a soft reference store that may get re-claimed when low on memory
            // as we want to ensure the producer is kept around, to ensure its lifecycle is fully managed,
            // eg stopping the producer when we stop etc.
            producerCache = new ProducerCache(this, endpoint.getVramelContext(), new HashMap<String, Producer>(1));
            // do not add as service as we do not want to manage the producer cache
        }
        ServiceHelper.startService(producerCache);
        return producerCache;
    }

    @Override
    public String toString() { 
        return "SendProcessor[endpoint: " + endpoint +"]";
    }

}
