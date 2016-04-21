/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.model;

import org.mule.runtime.core.api.MuleContext;

import java.util.List;

//TODO move to an SPI package and remove from spring and move to API
public interface ComponentBuildingDefinitionProvider
{

    void init(MuleContext muleContext);

    List<ComponentBuildingDefinition> getComponentBuildingDefinitions();

    //TODO hack for now
    boolean isDefaultConfigsProvider();

}
