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
package com.nxttxn.vramel;

/**
 * Allows objects to be notified when {@link VramelContext} has just been started.
 * <p/>
 * This can be used to perform any custom work when the entire {@link VramelContext} has been initialized and started.
 * For example this ensures that all Camel routes have been started and are up and running, before this callback
 * is invoked.
 * <p/>
 * For example the QuartzComponent leverages this to ensure the Quartz scheduler does not start until after all the
 * Camel routes and services have already been started.
 *
 * @version
 */
public interface StartupListener {

    /**
     * Callback invoked when the {@link VramelContext} has just been started.
     *
     * @param context        the Camel context
     * @param alreadyStarted whether or not the {@link VramelContext} already has been started. For example the context
     *                       could already have been started, and then a service is added/started later which still
     *                       triggers this callback to be invoked.
     * @throws Exception can be thrown in case of errors to fail the startup process and have the application
     *                   fail on startup.
     */
    void onVramelContextStarted(VramelContext context, boolean alreadyStarted) throws Exception;
}
