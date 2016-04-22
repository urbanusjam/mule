/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;

@Configuration(name = "listener-config")
@Providers(HttpListenerProvider.class)
@Sources(HttpListener.class)
public class HttpListenerConfig
{

    /**
     * Base path to use for all requests that reference this config.
     */
    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    private String basePath;

    //TODO: fix doc
    /**
     * By default, the request will be parsed (for example, a multi part request will be mapped as a
     * Mule message with null payload and inbound attachments with each part). If this property is set to false,
     * no parsing will be done, and the payload will always contain the raw contents of the HTTP request.
     */
    @Parameter
    @Optional(defaultValue = "true")
    @Expression(NOT_SUPPORTED)
    private boolean parseRequest;



}
