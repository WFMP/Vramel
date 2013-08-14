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

import com.nxttxn.vramel.spi.VramelContextNameStrategy;

import java.util.concurrent.atomic.AtomicInteger;



/**
 * A default name strategy which auto assigns a name using a prefix-counter pattern.
 *
 * @version
 */
public class DefaultVramelContextNameStrategy implements VramelContextNameStrategy {

    private static final AtomicInteger CONTEXT_COUNTER = new AtomicInteger(0);
    private final String prefix;
    private String name;

    public DefaultVramelContextNameStrategy() {
        this("camel");
    }

    public DefaultVramelContextNameStrategy(String prefix) {
        this.prefix = prefix;
        this.name = getNextName();
    }

    @Override
    public String getName() {
        if (name == null) {
            name = getNextName();
        }
        return name;
    }

    @Override
    public String getNextName() {
        return prefix + "-" + getNextCounter();
    }

    @Override
    public boolean isFixedName() {
        return false;
    }

    public static int getNextCounter() {
        return CONTEXT_COUNTER.incrementAndGet();
    }

    /**
     * To reset the counter, should only be used for testing purposes.
     *
     * @param value the counter value
     */
    public static void setCounter(int value) {
        CONTEXT_COUNTER.set(value);
    }

}
