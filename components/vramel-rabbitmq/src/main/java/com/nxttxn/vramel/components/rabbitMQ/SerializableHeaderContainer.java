package com.nxttxn.vramel.components.rabbitMQ;

import com.nxttxn.vramel.Message;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A Container of headers that implement the serializable interface.
 */
public class SerializableHeaderContainer {
    private Map<String, Object> headers;

    public SerializableHeaderContainer() {
        this.headers = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public SerializableHeaderContainer(byte[] bytes) {
        Object obj = SerializationUtils.deserialize(bytes);
        if (obj instanceof Map) {
            headers = (Map<String,Object>) obj;
        }
    }

    public void put(String key, Object value) {
        headers.put(key, value);
    }


    public boolean isEmpty() {
        return headers.isEmpty();
    }

    public byte[] serialize() {
        return SerializationUtils.serialize((Serializable) headers);
    }

    public void deserializeInto(Message message) {
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            message.setHeader(entry.getKey(), entry.getValue());
        }
    }
}
