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
package com.nxttxn.vramel.impl.converter;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.WrappedFile;
import com.nxttxn.vramel.components.bean.BeanInvocation;
import com.nxttxn.vramel.support.TypeConverterSupport;

import java.util.concurrent.Future;



/**
 * A simple converter that can convert any object to a String type by using the
 * toString() method of the object.
 *
 * @version
 */
public class ToStringTypeConverter extends TypeConverterSupport {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Class<T> toType, Exchange exchange, Object value) {
        if (value != null) {

            // should not try to convert Message
            if (Message.class.isAssignableFrom(value.getClass())) {
                return (T) Void.TYPE;
            }

            // should not try to convert future
            if (Future.class.isAssignableFrom(value.getClass())) {
                return (T) Void.TYPE;
            }

            // should not try to convert bean invocations
            if (BeanInvocation.class.isAssignableFrom(value.getClass())) {
                return (T) Void.TYPE;
            }

            // should not try to convert files
            if (WrappedFile.class.isAssignableFrom(value.getClass())) {
                return (T) Void.TYPE;
            }

            if (toType.equals(String.class)) {
                return (T)value.toString();
            }
        }
        return null;
    }

}