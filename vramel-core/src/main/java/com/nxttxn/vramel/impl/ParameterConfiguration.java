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

import com.nxttxn.vramel.spi.UriParam;

import java.lang.reflect.Field;


/**
 * Represents the configuration of a URI query parameter value to allow type conversion
 * and better validation of the configuration of URIs and Endpoints
 */
public class ParameterConfiguration {
    private final String name;
    private final Class<?> parameterType;

    public ParameterConfiguration(String name, Class<?> parameterType) {
        this.name = name;
        this.parameterType = parameterType;
    }

    @Override
    public String toString() {
        return "ParameterConfiguration[" + name + " on " + parameterType + "]";
    }

    /**
     * Returns the name of the parameter value
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the parameter value
     */
    public Class<?> getParameterType() {
        return parameterType;
    }

    /**
     * Factory method to create a new ParameterConfiguration from a field
     */
    public static ParameterConfiguration newInstance(String name, Field field, UriParam uriParam) {
        return new AnnotatedParameterConfiguration(name, field.getType(), field);
    }

}
