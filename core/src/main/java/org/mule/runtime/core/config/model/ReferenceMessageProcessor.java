/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.config.model;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.routing.MessageFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO see if this case can be improved without the need of this class - See usages of RuntimeBeanReference
public class ReferenceMessageProcessor implements Lifecycle, MuleContextAware, FlowConstructAware, MessageProcessor
{

    private static final Logger logger = LoggerFactory.getLogger(ReferenceMessageProcessor.class);
    private MessageProcessor messageProcessor;
    private Filter filter;
    private FlowConstruct flowConstruct;
    private MuleContext muleContext;

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(messageProcessor, logger);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (filter != null)
        {
            messageProcessor = new MessageFilter(filter);
        }
        LifecycleUtils.initialiseIfNeeded(messageProcessor, muleContext, flowConstruct);
    }

    @Override
    public void start() throws MuleException
    {
        LifecycleUtils.startIfNeeded(messageProcessor);
    }

    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(messageProcessor);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return messageProcessor.process(event);
    }

    public MessageProcessor getMessageProcessor()
    {
        return messageProcessor;
    }
}
