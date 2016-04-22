/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.authentication;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.module.http.api.HttpAuthentication;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestBuilder;

public class UsernamePasswordAuthentication implements HttpAuthentication
{
    @Parameter
    @Expression(NOT_SUPPORTED)
    private String username;

    @Parameter
    @Expression(NOT_SUPPORTED)
    private String password;

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    @Override
    public void authenticate(MuleEvent muleEvent, HttpRequestBuilder builder) throws MuleException
    {
        //do nothing
    }

    @Override
    public boolean shouldRetry(MuleEvent firstAttemptResponseEvent) throws MuleException
    {
        return false;
    }
}
