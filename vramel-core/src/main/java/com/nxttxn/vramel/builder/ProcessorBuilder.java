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
package com.nxttxn.vramel.builder;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Processor;

import java.io.Serializable;



/**
 * A builder of a number of different {@link com.nxttxn.vramel.AsyncProcessor} implementations
 *
 * @version
 */
public final class ProcessorBuilder {

    /**
     * Utility classes should not have a public constructor.
     */
    private ProcessorBuilder() {
    }

    /**
     * Creates a processor which sets the body of the IN message to the value of the expression
     */
    public static Processor setBody(final Expression expression) {
        return new Processor() {
            public void process(Exchange exchange) {
                Object newBody = expression.evaluate(exchange, Object.class);
                exchange.getIn().setBody(newBody);
            }

            @Override
            public String toString() {
                return "setBody(" + expression + ")";
            }
        };
    }

    /**
     * Creates a processor which sets the body of the OUT message to the value of the expression
     */
    public static Processor setOutBody(final Expression expression) {
        return new Processor() {
            public void process(Exchange exchange) {
                Object newBody = expression.evaluate(exchange, Object.class);
                exchange.getOut().setBody(newBody);
            }

            @Override
            public String toString() {
                return "setOutBody(" + expression + ")";
            }
        };
    }

    /**
     * Creates a processor which sets the body of the FAULT message to the value of the expression
     */
    public static Processor setFaultBody(final Expression expression) {
        return new Processor() {
            public void process(Exchange exchange) {
                Object newBody = expression.evaluate(exchange, Object.class);
                exchange.getOut().setFault(true);
                exchange.getOut().setBody(newBody);
            }

            @Override
            public String toString() {
                return "setFaultBody(" + expression + ")";
            }
        };
    }

    /**
     * Sets the header on the IN message
     */
    public static Processor setHeader(final String name, final Expression expression) {
        return new Processor() {
            public void process(Exchange exchange) {
                Serializable value = expression.evaluate(exchange, Serializable.class);
                exchange.getIn().setHeader(name, value);
            }

            @Override
            public String toString() {
                return "setHeader(" + name + ", " + expression + ")";
            }
        };
    }

    /**
     * Sets the header on the OUT message
     */
    public static Processor setOutHeader(final String name, final Expression expression) {
        return new Processor() {
            public void process(Exchange exchange) {
                Serializable value = expression.evaluate(exchange, Serializable.class);
                exchange.getOut().setHeader(name, value);
            }

            @Override
            public String toString() {
                return "setOutHeader(" + name + ", " + expression + ")";
            }
        };
    }

    /**
     * Sets the header on the FAULT message
     */
    public static Processor setFaultHeader(final String name, final Expression expression) {
        return new Processor() {
            public void process(Exchange exchange) {
                Serializable value = expression.evaluate(exchange, Serializable.class);
                exchange.getOut().setFault(true);
                exchange.getOut().setHeader(name, value);
            }

            @Override
            public String toString() {
                return "setFaultHeader(" + name + ", " + expression + ")";
            }
        };
    }

    /**
     * Sets the property on the exchange
     */
    public static Processor setProperty(final String name, final Expression expression) {
        return new Processor() {
            public void process(Exchange exchange) {
                Object value = expression.evaluate(exchange, Object.class);
                exchange.setProperty(name, value);
            }

            @Override
            public String toString() {
                return "setProperty(" + name + ", " + expression + ")";
            }
        };
    }

    /**
     * Removes the header on the IN message
     */
    public static Processor removeHeader(final String name) {
        return new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().removeHeader(name);
            }

            @Override
            public String toString() {
                return "removeHeader(" + name +  ")";
            }
        };
    }

//    /**
//     * Removes the headers on the IN message
//     */
//    public static AsyncProcessor removeHeaders(final String pattern) {
//        return new AsyncProcessor() {
//            public void process(Exchange exchange, OptionalAsyncResultHandler<Exchange> optionalAsyncResultHandler) {
//                exchange.getIn().removeHeaders(pattern);
//                optionalAsyncResultHandler.done(exchange);
//            }
//
//            @Override
//            public String toString() {
//                return "removeHeaders(" + pattern +  ")";
//            }
//        };
//    }
//
//    /**
//     * Removes all headers on the IN message, except for the ones provided in the <tt>names</tt> parameter
//     */
//    public static AsyncProcessor removeHeaders(final String pattern, final String... exceptionPatterns) {
//        return new AsyncProcessor() {
//            public void process(Exchange exchange, OptionalAsyncResultHandler<Exchange> optionalAsyncResultHandler) {
//                exchange.getIn().removeHeaders(pattern, exceptionPatterns);
//                optionalAsyncResultHandler.done(exchange);
//            }
//
//            @Override
//            public String toString() {
//                return "removeHeaders(" + pattern + ", " + Arrays.toString(exceptionPatterns) + ")";
//            }
//        };
//    }

    /**
     * Removes the header on the FAULT message
     * @deprecated will be removed in the near future. Instead use {@link #removeHeader(String)}
     */
    @Deprecated
    public static Processor removeFaultHeader(final String name) {
        return new Processor() {
            public void process(Exchange exchange) {
                exchange.getOut().setFault(true);
                exchange.getOut().removeHeader(name);
            }

            @Override
            public String toString() {
                return "removeFaultHeader(" + name +  ")";
            }
        };
    }

    /**
     * Removes the property on the exchange
     */
    public static Processor removeProperty(final String name) {
        return new Processor() {
            public void process(Exchange exchange) {
                exchange.removeProperty(name);
            }

            @Override
            public String toString() {
                return "removeProperty(" + name +  ")";
            }
        };
    }

    /**
     * Throws an exception
     */
    public static Processor throwException(final Exception ex) {
        return new Processor() {
            public void process(Exchange exchange) throws Exception {
                throw ex;
            }

            @Override
            public String toString() {
                return "throwException(" + ex.toString() +  ")";
            }
        };
    }
}
