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
package com.nxttxn.vramel.processor;


import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.ObjectHelper;

/**
 * The processor which sets an {@link Exception} on the {@link Exchange}
 */
public class ThrowExceptionProcessor  implements Processor {
    private final Exception exception;

    public ThrowExceptionProcessor(Exception exception) {
        ObjectHelper.notNull(exception, "exception", this);
        this.exception = exception;
    }



    public String toString() {
        return "ThrowException";
    }


    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.setException(exception);
    }
}