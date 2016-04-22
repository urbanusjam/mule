/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.http.api.requester.HttpStreamingType;
import org.mule.runtime.module.http.internal.listener.Server;

@Alias("listener")
public class HttpListener extends Source<Object, HttpRequestAttributes>
{
    @UseConfig
    private HttpListenerConfig listenerConfig;

    @Connection
    private Server server;

    @Parameter
    @Expression(NOT_SUPPORTED)
    private String path;

    /**
     * Comma separated list of allowed HTTP methods by this listener. To allow all methods do not defined the attribute.
     */
    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    private String allowedMethods;

    /**
     * Defines if the response should be sent using streaming or not. If this attribute is not present,
     * the behavior will depend on the type of the payload (it will stream only for InputStream). If set
     * to true, it will always stream. If set to false, it will never stream. As streaming is done the response
     * will be sent user Transfer-Encoding: chunked.
     */
    @Parameter
    @Optional(defaultValue = "AUTO")
    @Expression(NOT_SUPPORTED)
    private HttpStreamingType responseStreamingMode;

    /**
     * By default, the request will be parsed (for example, a multi part request will be mapped as a
     * Mule message with null payload and inbound attachments with each part). If this property is set to false,
     * no parsing will be done, and the payload will always contain the raw contents of the HTTP request.
     */
    @Parameter
    @Optional(defaultValue = "true")
    @Expression(NOT_SUPPORTED)
    private HttpStreamingType parseRequest;

    //TODO: Add response and error-response builders

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {

    }

    public void pepe()
    {

    }
}
