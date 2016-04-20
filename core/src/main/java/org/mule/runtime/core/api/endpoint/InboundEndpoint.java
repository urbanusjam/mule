/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.endpoint;

import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transport.MessageRequesting;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;

public interface InboundEndpoint
    extends ImmutableEndpoint, MessageRequesting, MessageSource, FlowConstructAware, Startable, Stoppable
{
    AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery);
}
