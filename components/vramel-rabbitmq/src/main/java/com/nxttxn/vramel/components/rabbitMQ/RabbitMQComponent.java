/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nxttxn.vramel.components.rabbitMQ;

import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.UriEndpointComponent;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import javax.net.ssl.TrustManager;

public class RabbitMQComponent extends UriEndpointComponent {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQComponent.class);

    public RabbitMQComponent() {
        super(RabbitMQEndpoint.class);
    }

    public RabbitMQComponent(VramelContext context) {
        super(context, RabbitMQEndpoint.class);
    }

    @Override
    protected RabbitMQEndpoint createEndpoint(String uri,
                                              String remaining,
                                              Map<String, Object> params) throws Exception {
        URI host = new URI("http://" + remaining);
        String hostname = host.getHost();
        int portNumber = host.getPort();
        if (host.getPath().trim().length() <= 1) {
            throw new IllegalArgumentException("No URI path as the exchangeName for the RabbitMQEndpoint, the URI is " + uri);
        }
        String exchangeName = host.getPath().substring(1);

        // ConnectionFactory reference
        ConnectionFactory connectionFactory = resolveAndRemoveReferenceParameter(params, "connectionFactory", ConnectionFactory.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> clientProperties = resolveAndRemoveReferenceParameter(params, "clientProperties", Map.class);
        TrustManager trustManager = resolveAndRemoveReferenceParameter(params, "trustManager", TrustManager.class);
        RabbitMQEndpoint endpoint;
        if (connectionFactory == null) {
            endpoint = new RabbitMQEndpoint(uri, this);
        } else {
            endpoint = new RabbitMQEndpoint(uri, this, connectionFactory);
        }
        endpoint.setHostname(hostname);
        endpoint.setPortNumber(portNumber);
        endpoint.setExchangeName(exchangeName);
        endpoint.setClientProperties(clientProperties);
        endpoint.setTrustManager(trustManager);
        setProperties(endpoint, params);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating RabbitMQEndpoint with host {}:{} and exchangeName: {}",
                    new Object[]{endpoint.getHostname(), endpoint.getPortNumber(), endpoint.getExchangeName()});
        }

        return endpoint;
    }
}
