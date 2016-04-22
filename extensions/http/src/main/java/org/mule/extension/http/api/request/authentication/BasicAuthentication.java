/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.authentication;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

public class BasicAuthentication extends UsernamePasswordAuthentication
{
    @Parameter
    @Optional(defaultValue = "true")
    @Expression(NOT_SUPPORTED)
    private boolean preemptive;

    public boolean isPreemptive()
    {
        return preemptive;
    }
}
