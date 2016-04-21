/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.model;

import org.mule.runtime.core.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the mapping between a component configuration and how the object that represents
 * that model in runtime is created.
 */
public class ComponentBuildingDefinition
{

    private String name;
    private TypeDefinitionBuilder typeDefinitionBuilder;
    private boolean scope;
    private String namespace;
    private List<ParameterDefinition> constructorParameterDefinition = new ArrayList<>();
    private Map<String, ParameterDefinition> setterParameterDefinitions = new HashMap<>();
    private Class<?> objectFactoryType;
    private boolean prototype;

    private ComponentBuildingDefinition()
    {
    }

    public TypeDefinitionBuilder getTypeDefinitionBuilder()
    {
        return typeDefinitionBuilder;
    }

    public boolean isScope()
    {
        return scope;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public String getName()
    {
        return name;
    }

    public List<ParameterDefinition> getConstructorParameterDefinition()
    {
        return constructorParameterDefinition;
    }

    public Map<String, ParameterDefinition> getSetterParameterDefinitions()
    {
        return setterParameterDefinitions;
    }

    public Class<?> getObjectFactoryType()
    {
        return objectFactoryType;
    }

    public boolean isPrototype()
    {
        return prototype;
    }

    public static class Builder
    {
        private ComponentBuildingDefinition definition = new ComponentBuildingDefinition();

        public Builder withConstructorParameterDefinition(ParameterDefinition parameterDefinition)
        {
            definition.constructorParameterDefinition.add(parameterDefinition);
            return this;
        }

        public Builder withSetterParameterDefinition(String fieldName, ParameterDefinition parameterDefinition)
        {
            definition.setterParameterDefinitions.put(fieldName, parameterDefinition);
            return this;
        }

        public Builder withNamespace(String namespace)
        {
            definition.namespace = namespace;
            return this;
        }

        public Builder withName(String name)
        {
            definition.name = name;
            return this;
        }

        public Builder withTypeDefinitionBuilder(TypeDefinitionBuilder typeDefinitionBuilder)
        {
            definition.typeDefinitionBuilder = typeDefinitionBuilder;
            return this;
        }

        public Builder asScope()
        {
            definition.scope = true;
            return this;
        }

        public ComponentBuildingDefinition build()
        {
            Preconditions.checkState(definition.typeDefinitionBuilder != null, "You must specify the type");
            Preconditions.checkState(definition.name != null, "You must specify the name");
            Preconditions.checkState(definition.namespace != null, "You must specify the namespace");
            return definition;
        }

        //TODO refactor this method and the type method so it does not change the meaning of which object gets injected and created.
        public Builder withObjectFactoryType(Class<?> objectFactoryType)
        {
            definition.objectFactoryType = objectFactoryType;
            return this;
        }

        public Builder copy()
        {
            Builder builder = new Builder();
            builder.definition.setterParameterDefinitions = new HashMap<>(this.definition.setterParameterDefinitions);
            builder.definition.constructorParameterDefinition = new ArrayList<>(this.definition.constructorParameterDefinition);
            builder.definition.name = this.definition.name;
            builder.definition.namespace = this.definition.namespace;
            builder.definition.scope = this.definition.scope;
            builder.definition.typeDefinitionBuilder = this.definition.typeDefinitionBuilder;
            return builder;
        }

        //TODO, remove for some other sometic. Like sub-flow being just a language construct that later a process regenerates the
        //component definition model creating an effective one.
        public Builder asPrototype()
        {
            definition.prototype = true;
            return this;
        }
    }
}
