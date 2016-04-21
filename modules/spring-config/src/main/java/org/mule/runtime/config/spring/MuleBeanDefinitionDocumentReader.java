/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.config.spring.model.ApplicationConfig;
import org.mule.runtime.config.spring.model.ApplicationModel;
import org.mule.runtime.config.spring.model.BeanDefinitionFactory;
import org.mule.runtime.config.spring.model.ComponentDefinitionModel;
import org.mule.runtime.config.spring.model.ConfigFile;
import org.mule.runtime.config.spring.model.ConfigLine;
import org.mule.runtime.config.spring.model.XmlApplicationElementParser;

import java.util.ArrayList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Allows us to hook in our own Hierarchical Parser delegate. this enables the
 * parsing of custom spring bean elements nested within each other
 */
public class MuleBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader
{

    private BeanDefinitionFactory beanDefinitionFactory;
    private XmlApplicationElementParser xmlApplicationElementParser = new XmlApplicationElementParser();
    private ApplicationModel oldApplicationModel;
    private ApplicationModel applicationModel;

    public MuleBeanDefinitionDocumentReader(BeanDefinitionFactory beanDefinitionFactory)
    {
        this.beanDefinitionFactory = beanDefinitionFactory;
    }

    @Override
    protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate)
    {
        BeanDefinitionParserDelegate delegate = createBeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    protected MuleHierarchicalBeanDefinitionParserDelegate createBeanDefinitionParserDelegate(XmlReaderContext readerContext)
    {
        return new MuleHierarchicalBeanDefinitionParserDelegate(readerContext, this, () -> { return applicationModel;}, beanDefinitionFactory);
    }

    /* Keep backward compatibility with spring 3.0 */
    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root)
    {
        BeanDefinitionParserDelegate delegate = createBeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root);
        return delegate;
    }

    /**
     * Override to reject configuration files with no namespace, e.g. mule legacy
     * configuration file.
     */
    @Override
    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate)
    {
        if (!StringUtils.hasLength(root.getNamespaceURI()))
        {
            getReaderContext().error("Unable to locate NamespaceHandler for namespace [null]", root);
        }
        else
        {
            super.parseBeanDefinitions(root, delegate);
        }
    }

    @Override
    protected void preProcessXml(Element root)
    {
        oldApplicationModel = applicationModel;
        ArrayList<ConfigLine> configLines = new ArrayList<>();
        configLines.add(xmlApplicationElementParser.parse(root).get());
        ApplicationConfig applicationConfig = new ApplicationConfig.Builder().addConfigFile(new ConfigFile("fakeName", configLines)).build();
        applicationModel = new ApplicationModel(applicationConfig);
    }

    @Override
    protected void postProcessXml(Element root)
    {
        applicationModel = oldApplicationModel;
    }

    public void setBeanDefinitionFactory(BeanDefinitionFactory beanDefinitionFactory)
    {
        this.beanDefinitionFactory = beanDefinitionFactory;
    }

}
