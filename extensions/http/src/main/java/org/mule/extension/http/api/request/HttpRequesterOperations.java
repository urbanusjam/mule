/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request;

import org.mule.extension.http.api.HttpConnector;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.module.http.api.requester.HttpSendBodyMode;
import org.mule.runtime.module.http.api.requester.HttpStreamingType;
import org.mule.runtime.module.http.internal.request.FailureStatusCodeValidator;
import org.mule.runtime.module.http.internal.request.SuccessStatusCodeValidator;
import org.mule.runtime.module.http.internal.request.grizzly.GrizzlyHttpClient;

public class HttpRequesterOperations
{

    /**
     * Consumes an HTTP service.
     *
     * @param path Path where the request will be sent.
     * @param method The HTTP method for the request.
     * @param host Host where the requests will be sent.
     * @param port Port where the requests will be sent.
     * @param source The expression used to obtain the body that will be sent in the request. Default is empty, so the payload will be used as the body.
     * @param followRedirects Specifies whether to follow redirects or not.
     * @param parseResponse Defines if the HTTP response should be parsed or it's raw contents should be propagated instead.
     * @param requestStreamingMode Defines if the request should be sent using streaming or not.
     * @param sendBodyMode Defines if the request should contain a body or not.
     * @param responseTimeout Maximum time that the request element will block the execution of the flow waiting for the HTTP response.
     * @param successStatusCodeValidator Configures error handling of the response based on the status codes considered successful.
     * @param failureStatusCodeValidator Configures error handling of the response based on the status codes considered failures.
     * @param config the {@link HttpConnector} configuration for this operation. All parameters not configured will be taken from it.
     * @param muleEvent the current {@link MuleEvent}
     * @return a {@link MuleMessage} with {@link HttpResponseAttributes}
     */
    public MuleMessage<Object, HttpResponseAttributes> request(String path,
                                                          @Optional(defaultValue = "GET") String method,
                                                          @Optional String host,
                                                          @Optional Integer port,
                                                          @Optional String source,
                                                          @Optional boolean followRedirects,
                                                          @Optional boolean parseResponse,
                                                          @Optional HttpStreamingType requestStreamingMode,
                                                          @Optional HttpSendBodyMode sendBodyMode,
                                                          @Optional int responseTimeout,
                                                          @Optional SuccessStatusCodeValidator successStatusCodeValidator,
                                                          @Optional FailureStatusCodeValidator failureStatusCodeValidator,
                                                          @Connection GrizzlyHttpClient client,
                                                          @UseConfig HttpRequesterConfig config,
                                                          MuleEvent muleEvent)
    {
        //TODO: Add request builder
        return null;
    }

}
