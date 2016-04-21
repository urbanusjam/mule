/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.api.GlobalNameableObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO Improve so delegation of start and stop and flow construct injection is not necessary
//TODO see if we can get rid of this class and revert this change
public class MessagingExceptionStrategyRef implements MessagingExceptionHandlerAcceptor, Lifecycle, FlowConstructAware, MessageProcessorContainer, GlobalNameableObject, MuleContextAware
{
    private Logger logger = LoggerFactory.getLogger(MessagingExceptionStrategyRef.class);
    private MessagingExceptionHandler delegate;
    private FlowConstruct flowConstruct;
    private MuleContext muleContext;

    public MessagingExceptionStrategyRef(MessagingExceptionHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public MuleEvent handleException(Exception exception, MuleEvent event) {
        return delegate.handleException(exception, event);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void start() throws MuleException
    {
        LifecycleUtils.startIfNeeded(delegate);
    }

    @Override
    public void stop() throws MuleException {
        LifecycleUtils.stopIfNeeded(delegate);
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        if (delegate instanceof MessageProcessorContainer)
        {
            ((MessageProcessorContainer) delegate).addMessageProcessorPathElements(pathElement);
        }
    }

    @Override
    public String getGlobalName()
    {
        if (delegate instanceof GlobalNameableObject)
        {
            return ((GlobalNameableObject) delegate).getGlobalName();
        }
        return null;
    }

    @Override
    public void setGlobalName(String name)
    {
        if (delegate instanceof GlobalNameableObject)
        {
            ((GlobalNameableObject) delegate).setGlobalName(name);
        }
    }

    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(delegate, logger);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(delegate, muleContext, flowConstruct);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public boolean accept(MuleEvent event)
    {
        if (delegate instanceof MessagingExceptionHandlerAcceptor)
        {
            return ((MessagingExceptionHandlerAcceptor) delegate).accept(event);
        }
        return true;
    }

    @Override
    public boolean acceptsAll()
    {
        if (delegate instanceof MessagingExceptionHandlerAcceptor)
        {
            return ((MessagingExceptionHandlerAcceptor) delegate).acceptsAll();
        }
        return true;
    }
}
