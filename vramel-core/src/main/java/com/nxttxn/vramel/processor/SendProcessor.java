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


/**
 * Processor for forwarding exchanges to an endpoint destination.
 *
 * @version
 */
public class SendProcessor extends ServiceSupport implements AsyncProcessor {
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
        getProducerCache();
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopService(producerCache);
    }

    private ProducerCache getProducerCache() throws Exception {
        if (producerCache == null) {
            producerCache = new ProducerCache(this, endpoint.getVramelContext());

        }
        if (!producerCache.isStarted()) {
            ServiceHelper.startService(producerCache);
        }
        return producerCache;
    }
}
