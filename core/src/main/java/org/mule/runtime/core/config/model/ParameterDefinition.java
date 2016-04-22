/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.model;

public class ParameterDefinition
{
    private String parameterName;
    private Object defaultValue;
    private boolean hasDefaultValue;
    private boolean undefinedSimpleParametersHolder;
    private Class<?> referenceObject;
    private Class<?> childObjectType;
    private boolean undefinedComplexParametersHolder;
    private String referenceSimpleParameter;
    private boolean collection;
    private String mapIdentifier;
    private boolean valueFromTextContent;

    public void accept(ParameterDefinitionVisitor visitor)
    {
        if (parameterName != null)
        {
            visitor.onConfigurationParameter(parameterName, defaultValue);
        }
        else if (referenceObject != null)
        {
            visitor.onReferenceObject(referenceObject);
        }
        else if (hasDefaultValue)
        {
            visitor.onProvidedValue(defaultValue);
        }
        else if (undefinedSimpleParametersHolder)
        {
            visitor.onUndefinedSimpleParameters();
        }
        else if (undefinedComplexParametersHolder)
        {
            visitor.onUndefinedComplexParameters();
        }
        else if (referenceSimpleParameter != null)
        {
            visitor.onReferenceSimpleParameter(referenceSimpleParameter);
        }
        else if (childObjectType != null && collection)
        {
            visitor.onComplexChildList(childObjectType);
        }
        else if (childObjectType != null)
        {
            visitor.onComplexChild(childObjectType);
        }
        else if (valueFromTextContent)
        {
            visitor.onValueFromTextContent();
        }
        else
        {
            throw new RuntimeException();
        }
    }

    public static class Builder
    {

        private ParameterDefinition parameterDefinition = new ParameterDefinition();

        private Builder()
        {
        }

        public static Builder fromSimpleParameter(String parameterName)
        {
            Builder builder = new Builder();
            builder.parameterDefinition.parameterName = parameterName;
            return builder;
        }

        /**
         * Once this method gets called, even if the parameter is null, it will use it as default value.
         *
         * @param defaultValue
         * @return
         */
        public Builder withDefaultValue(Object defaultValue)
        {
            parameterDefinition.hasDefaultValue = true;
            parameterDefinition.defaultValue = defaultValue;
            return this;
        }

        public ParameterDefinition build()
        {
            return parameterDefinition;
        }

        public static Builder fromNoConfiguration()
        {
            return new Builder();
        }

        public static Builder fromUndefinedSimpleAttributes()
        {
            Builder builder = new Builder();
            builder.parameterDefinition.undefinedSimpleParametersHolder = true;
            return builder;
        }

        public static Builder fromReferenceObject(Class<?> referenceObjectType)
        {
            Builder builder = new Builder();
            builder.parameterDefinition.referenceObject = referenceObjectType;
            return builder;
        }

        public static Builder fromChildConfiguration(Class<?> childType)
        {
            Builder builder = new Builder();
            builder.parameterDefinition.childObjectType = childType;
            return builder;
        }

        public static Builder fromUndefinedComplexAttribute()
        {
            Builder builder = new Builder();
            builder.parameterDefinition.undefinedComplexParametersHolder = true;
            return builder;
        }

        public static Builder fromSimpleReferenceParameter(String referenceSimpleParameter)
        {
            Builder builder = new Builder();
            builder.parameterDefinition.referenceSimpleParameter = referenceSimpleParameter;
            return builder;
        }

        public static Builder fromChildListConfiguration(Class<?> type)
        {
            Builder builder = new Builder();
            builder.parameterDefinition.childObjectType = type;
            builder.parameterDefinition.collection = true;
            return builder;
        }

        public static Builder fromChildMapConfiguration(String mapIdentifier)
        {
            Builder builder = new Builder();
            builder.parameterDefinition.mapIdentifier = mapIdentifier;
            return builder;
        }

        public static Builder fromTextContent()
        {
            Builder builder = new Builder();
            builder.parameterDefinition.valueFromTextContent = true;
            return builder;
        }
    }

}
