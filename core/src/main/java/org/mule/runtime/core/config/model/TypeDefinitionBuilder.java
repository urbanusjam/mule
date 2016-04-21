/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.config.model;

public class TypeDefinitionBuilder
{
    private Class<?> type;
    private String attributeName;

    public static class Builder
    {

        private Builder() {}

        public static TypeDefinitionBuilder fromType(Class<?> type)
        {
            TypeDefinitionBuilder typeDefinitionBuilder = new TypeDefinitionBuilder();
            typeDefinitionBuilder.type = type;
            return typeDefinitionBuilder;
        }

        public static TypeDefinitionBuilder fromConfigurationAttribute(String attributeName)
        {
            TypeDefinitionBuilder typeDefinitionBuilder = new TypeDefinitionBuilder();
            typeDefinitionBuilder.attributeName = attributeName;
            return typeDefinitionBuilder;
        }
    }

    public void visit(TypeDefinitionBuilderVisitor typeDefinitionBuilderVisitor)
    {
        if (type != null)
        {
            typeDefinitionBuilderVisitor.onType(type);
        }
        else
        {
            typeDefinitionBuilderVisitor.onConfigurationAttribute(attributeName);
        }
    }
}
