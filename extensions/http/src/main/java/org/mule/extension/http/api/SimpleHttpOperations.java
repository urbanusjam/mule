/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.extension.api.annotation.param.Optional;

public class SimpleHttpOperations
{
    //TODO: Analyse moving this as inner elements of the listener source to avoid having to produce old MuleMessage to maintain outbound property use

    //receives a request, return a proper response
    public MuleMessage<?, HttpResponseAttributes> loadStaticResource(MuleMessage<?, HttpRequestAttributes> message,
                                                                     String resourceBase,
                                                                     @Optional(defaultValue = "index.html") String defaultFile)
    {
        //composite or copy of StaticResourceMessageProcessor
        return null;
    }

    //receives a request, return the request attributes if successful and response attributes otherwise
    public MuleMessage<?, HttpAttributes> basicSecurityFilter(MuleMessage<?, HttpRequestAttributes> message,
                                                              String realm,
                                                              @Optional String securityProviders)
    {
        //composite or copy of HttpBasicAuthenticationFilter
        return null;
    }

}
