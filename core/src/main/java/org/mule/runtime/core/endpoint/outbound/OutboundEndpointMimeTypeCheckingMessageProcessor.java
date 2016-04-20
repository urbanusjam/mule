/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.outbound;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transport.PropertyScope;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.ObjectUtils;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;


/**
 * Verify that the outbound mime type is acceptable by this endpoint.
 */
public class OutboundEndpointMimeTypeCheckingMessageProcessor implements MessageProcessor
{
    private OutboundEndpoint endpoint;

    public OutboundEndpointMimeTypeCheckingMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MessagingException
    {
        String endpointMimeType = endpoint.getMimeType();
        if (endpointMimeType != null)
        {
            MuleMessage message = event.getMessage();
            String contentType = message.getProperty(MuleProperties.CONTENT_TYPE_PROPERTY, PropertyScope.OUTBOUND);
            if (contentType == null)
            {
                message.setProperty(MuleProperties.CONTENT_TYPE_PROPERTY, endpointMimeType, PropertyScope.OUTBOUND);
            }
            else
            {
                try
                {
                    MimeType mt = new MimeType(contentType);
                    String messageMimeType = mt.getPrimaryType() + "/" + mt.getSubType();
                    if (!messageMimeType.equals(endpointMimeType))
                    {
                        throw new MessagingException(
                            CoreMessages.unexpectedMIMEType(messageMimeType, endpointMimeType), event, this);
                    }
                }
                catch (MimeTypeParseException ex)
                {
                    throw new MessagingException(CoreMessages.illegalMIMEType(contentType), event, ex, this);
                }
            }
        }

        return event;
    }
    
    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
