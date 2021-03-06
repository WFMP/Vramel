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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.nxttxn.vramel.model.ProcessorDefinitionHelper;
import com.nxttxn.vramel.processor.exceptionpolicy.DefaultExceptionPolicyStrategy;
import com.nxttxn.vramel.processor.exceptionpolicy.ExceptionPolicyKey;
import com.nxttxn.vramel.processor.exceptionpolicy.ExceptionPolicyStrategy;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.support.ChildServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for {@link ErrorHandler} implementations.
 *
 * @version
 */
public abstract class ErrorHandlerSupport extends ChildServiceSupport implements ErrorHandler {

    protected final transient Logger log = LoggerFactory.getLogger(getClass());

    protected final Map<ExceptionPolicyKey, OnExceptionDefinition> exceptionPolicies = new LinkedHashMap<ExceptionPolicyKey, OnExceptionDefinition>();
    protected ExceptionPolicyStrategy exceptionPolicy = createDefaultExceptionPolicyStrategy();

    public void addExceptionPolicy(FlowContext flowContext, OnExceptionDefinition exceptionType) {
//        if (flowContext != null) {
//            // add error handler as child service so they get lifecycle handled
//            AsyncProcessor errorHandler = exceptionType.getErrorHandler(flowContext.getFlow().getId());
//            if (errorHandler != null) {
//                addChildService(errorHandler);
//            }
//        }

        List<Class<? extends Throwable>> list = exceptionType.getExceptionClasses();
        for (Class<? extends Throwable> clazz : list) {
            String routeId = null;
            // only get the route id, if the exception type is route scoped
            if (exceptionType.isFlowScoped()) {
                FlowDefinition flow = ProcessorDefinitionHelper.getFlow(exceptionType);
                if (flow != null) {
                    routeId = flow.getId();
                }
            }
            ExceptionPolicyKey key = new ExceptionPolicyKey(routeId, clazz, exceptionType.getOnWhen());
            exceptionPolicies.put(key, exceptionType);
        }
    }

    /**
     * Attempts to find the best suited {@link OnExceptionDefinition} to be used for handling the given thrown exception.
     *
     * @param exchange  the exchange
     * @param exception the exception that was thrown
     * @return the best exception type to handle this exception, <tt>null</tt> if none found.
     */
    protected OnExceptionDefinition getExceptionPolicy(Exchange exchange, Throwable exception) {
        if (exceptionPolicy == null) {
            throw new IllegalStateException("The exception policy has not been set");
        }

        return exceptionPolicy.getExceptionPolicy(exceptionPolicies, exchange, exception);
    }

    /**
     * Sets the strategy to use for resolving the {@link OnExceptionDefinition} to use
     * for handling thrown exceptions.
     */
    public void setExceptionPolicy(ExceptionPolicyStrategy exceptionPolicy) {
        if (exceptionPolicy != null) {
            this.exceptionPolicy = exceptionPolicy;
        }
    }

    /**
     * Creates the default exception policy strategy to use.
     */
    public static ExceptionPolicyStrategy createDefaultExceptionPolicyStrategy() {
        return new DefaultExceptionPolicyStrategy();
    }


    /**
     * Gets the output
     */
    public abstract Processor getOutput();

}
