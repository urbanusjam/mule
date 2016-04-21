/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.config.model;

public interface ParameterDefinitionVisitor
{

    void onReferenceObject(Class<?> objectType);

    void onReferenceSimpleParameter(String reference);

    void onProvidedValue(Object value);

    void onConfigurationParameter(String parameterName, Object defaultValue);

    void onUndefinedSimpleParameters();

    void onUndefinedComplexParameters();

    void onComplexChildList(Class<?> type);

    void onComplexChild(Class<?> type);

    void onValueFromTextContent();
}
