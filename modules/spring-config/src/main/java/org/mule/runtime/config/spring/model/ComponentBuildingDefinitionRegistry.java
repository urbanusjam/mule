/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import org.mule.runtime.core.config.model.ComponentBuildingDefinition;
import org.mule.runtime.core.config.model.ComponentIdentifier;

import java.util.HashMap;
import java.util.Map;

public class ComponentBuildingDefinitionRegistry
{

    private Map<ComponentIdentifier, ComponentBuildingDefinition> builderDefinitionsMap = new HashMap<>();

    public void register(ComponentBuildingDefinition builderDefinition) {
        builderDefinitionsMap.put(new ComponentIdentifier(builderDefinition.getNamespace(), builderDefinition.getName()), builderDefinition);
    }

    public ComponentBuildingDefinition getBuildingDefinition(String namespace, String identifier)
    {
        return builderDefinitionsMap.get(new ComponentIdentifier(namespace, identifier));
    }
}
