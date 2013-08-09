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
package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Component;
import com.nxttxn.vramel.ComponentConfiguration;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.URISupport;
import com.nxttxn.vramel.util.UnsafeUriCharactersEncoder;

import java.net.URISyntaxException;
import java.util.*;


/**
 * Useful base class for implementations of {@link ComponentConfiguration}
 */
public abstract class ComponentConfigurationSupport implements ComponentConfiguration {
    protected final Component component;
    private Map<String, Object> propertyValues = new HashMap<String, Object>();
    private String baseUri;

    public ComponentConfigurationSupport(Component component) {
        this.component = component;
    }

    @Override
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(propertyValues);
    }

    @Override
    public void setParameters(Map<String, Object> newValues) {
        ObjectHelper.notNull(newValues, "propertyValues");
        this.propertyValues.clear();
        // lets validate each property as we set it
        Set<Map.Entry<String, Object>> entries = newValues.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            setParameter(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object getParameter(String name) {
        validatePropertyName(name);
        return propertyValues.get(name);
    }

    @Override
    public void setParameter(String name, Object value) {
        Object convertedValue = validatePropertyValue(name, value);
        propertyValues.put(name, convertedValue);
    }

    /**
     * Returns the base URI without any scheme or URI query parameters (property values)
     */
    @Override
    public String getBaseUri() {
        return baseUri;
    }

    @Override
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public Endpoint createEndpoint() throws Exception {
        String uri = getUriString();
        return component.createEndpoint(uri);
    }

    /**
     * Configures the properties on the given endpoint
     */
    @Override
    public void configureEndpoint(Endpoint endpoint) {
        Map<String, Object> map = getParameters();
        if (map != null) {
            Set<Map.Entry<String, Object>> entries = map.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                setEndpointParameter(endpoint, entry.getKey(), entry.getValue());
            }
        }
        // TODO validate all the values are valid (e.g. mandatory)
    }

    @Override
    public String getUriString() {
        List<String> queryParams = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : getParameters().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // convert to "param=value" format here, order will be preserved
            if (value instanceof List) {
                for (Object item : (List<?>)value) {
                    queryParams.add(key + "=" + UnsafeUriCharactersEncoder.encode(item.toString()));
                }
            } else {
                queryParams.add(key + "=" + UnsafeUriCharactersEncoder.encode(value.toString()));
            }
        }
        Collections.sort(queryParams);
        StringBuilder builder = new StringBuilder();
        String base = getBaseUri();
        if (base != null) {
            builder.append(base);
        }
        String separator = "?";
        for (String entry : queryParams) {
            builder.append(separator);
            builder.append(entry);
            separator = "&";
        }
        return builder.toString();
    }

    @Override
    public void setUriString(String uri) throws URISyntaxException {
        String path = uri;
        int idx = path.indexOf('?');
        Map<String, Object> newParameters = Collections.EMPTY_MAP;
        if (idx >= 0) {
            path = path.substring(0, idx);
            String query = uri.substring(idx + 1);
            newParameters = URISupport.parseQuery(query, true);
        }
        setBaseUri(path);
        setParameters(newParameters);
    }

    @Override
    public ParameterConfiguration getParameterConfiguration(String name) {
        return getParameterConfigurationMap().get(name);
    }

    /**
     * Allow implementations to validate whether a property name is valid
     * and either throw an exception or log a warning of an unknown property being used
     */
    protected void validatePropertyName(String name) {
    }

    /**
     * Allow implementations to validate whether a property name is valid
     * and either throw an exception or log a warning of an unknown property being used
     * and to convert the given value to the correct type before updating the value.
     */
    protected Object validatePropertyValue(String name, Object value) {
        validatePropertyName(name);
        return value;
    }

}
