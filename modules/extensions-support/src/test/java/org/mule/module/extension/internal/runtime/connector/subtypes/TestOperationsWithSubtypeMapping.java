/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.subtypes;

import org.mule.extension.api.annotation.param.Connection;
import org.mule.extension.api.annotation.param.UseConfig;

import java.util.Arrays;
import java.util.List;

public class TestOperationsWithSubtypeMapping
{

    public Shape shapeRetriever(Shape shape)
    {
        return shape;
    }

    public Door doorRetriever(Door door)
    {
        return door;
    }

    public SubTypesMappingConnector configRetriever(@UseConfig SubTypesMappingConnector config)
    {
        return config;
    }

    public SubtypesConnectorConnection connectionRetriever(@Connection SubtypesConnectorConnection connection)
    {
        return connection;
    }

    public List<Object> subtypedAndConcreteParameters(Shape shapeParam, Door doorParam, FinalPojo finalPojoParam)
    {
        return Arrays.asList(shapeParam, doorParam, finalPojoParam);
    }
}