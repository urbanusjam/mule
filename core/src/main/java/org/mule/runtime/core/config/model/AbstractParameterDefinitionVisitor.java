/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.config.model;

public class AbstractParameterDefinitionVisitor implements ParameterDefinitionVisitor
{
    @Override
    public void onReferenceObject(Class<?> objectType)
    {
    }

    @Override
    public void onReferenceSimpleParameter(String reference)
    {

    }

    @Override
    public void onProvidedValue(Object value)
    {
    }

    @Override
    public void onConfigurationParameter(String parameterName, Object defaultValue)
    {

    }


    @Override
    public void onUndefinedSimpleParameters()
    {
    }

    @Override
    public void onUndefinedComplexParameters()
    {

    }

    @Override
    public void onComplexChildList(Class<?> type)
    {

    }

    @Override
    public void onComplexChild(Class<?> type)
    {

    }

    @Override
    public void onValueFromTextContent()
    {

    }
}
