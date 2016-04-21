/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

public class ConfigAttribute
{
    //TODO this should be a generic map. Not an specific field since it's XML specific
    private String namespaceUri;
    private String name;
    private String value;

    public ConfigAttribute(String namespaceUri, String name, String value)
    {
        this.namespaceUri = namespaceUri;
        this.name = name;
        this.value = value;
    }

    public String getNamespaceUri()
    {
        return namespaceUri;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }
}
