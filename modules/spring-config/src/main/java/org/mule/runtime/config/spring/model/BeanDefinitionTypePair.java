/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.model;

import org.springframework.beans.factory.config.BeanDefinition;

public class BeanDefinitionTypePair
{

    private Class<?> type;
    private BeanDefinition beanDefinition;

    public BeanDefinitionTypePair(Class<?> type, BeanDefinition beanDefinition)
    {
        this.type = type;
        this.beanDefinition = beanDefinition;
    }

    public Class<?> getType()
    {
        return type;
    }

    public BeanDefinition getBeanDefinition()
    {
        return beanDefinition;
    }
}
