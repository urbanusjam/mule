/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;


import org.mule.runtime.core.api.config.MuleProperties;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 *
 */
public class MuleContextSetterAttributeBinder implements DefaultAttributeBinder {
    @Override
    public void apply(BeanDefinitionBuilder beanDefinitionBuilder) {
        beanDefinitionBuilder.addPropertyReference("muleContext", MuleProperties.OBJECT_MULE_CONTEXT);
    }
}
