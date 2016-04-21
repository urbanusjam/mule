package org.mule.runtime.core.config.model;/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

public interface TypeDefinitionBuilderVisitor
{
    /**
     * Invoked when the {@link org.mule.config.spring.language.TypeDefinitionBuilder} it's defined
     * from a {@code Class} hardcoded value
     * @param type the hardcoded type
     */
    void onType(Class<?> type);

    /**
     * Invoked when the {@link org.mule.config.spring.language.TypeDefinitionBuilder} it's defined
     * from a configuration attribute of the component
     * @param attributeName the name of the configuration attribute holding the type definition. Most likely a fully qualified java class name.
     */
    void onConfigurationAttribute(String attributeName);

}
