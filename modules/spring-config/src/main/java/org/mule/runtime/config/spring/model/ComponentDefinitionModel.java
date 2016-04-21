/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageRouter;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Node;

/**
 * An ComponentDefinitionModel represents the definition of a component (flow, config, message processor, etc) in the
 * mule artifact configuration.
 *
 * TODO: seems this can be removed or ConfigLine can be removed
 */
public class ComponentDefinitionModel
{

    private boolean root = false;
    private String namespace = "mule";
    private String identifier;
    private Map<String, Serializable> metadata = new HashMap<>();
    private Map<String, String> attributes = new HashMap<>();
    private List<ComponentDefinitionModel> innerComponents = new ArrayList<>();
    //TODO remove once the old parsing mechanism is not needed anymore
    private Node node;
    private String textContent;
    private String namespaceUri;

    public ComponentDefinitionModel()
    {
    }

    //TODO remove this attributes
    private BeanDefinition beanDefinition;
    private Class<?> type;

    public String getIdentifier() {
        return identifier;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getAttributes()
    {
        return Collections.unmodifiableMap(attributes);
    }

    public List<ComponentDefinitionModel> getInnerComponents() {
        return innerComponents;
    }

    public List<ComponentDefinitionModel> getInnerMessageProcessorModels() {
        return innerComponents.stream().filter(component -> MessageProcessor.class.isAssignableFrom(component.getType())).collect(Collectors.toList());
    }

    public Map<String, Serializable> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public boolean isRoot() {
        return root;
    }

    public void setBeanDefinition(BeanDefinition beanDefinition) {
        this.beanDefinition = beanDefinition;
    }

    public BeanDefinition getBeanDefinition() {
        return beanDefinition;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getNameAttribute() {
        return attributes.get("name");
    }

    public boolean isScope() {
        return MessageRouter.class.isAssignableFrom(type);
    }

    public Node getNode()
    {
        return node;
    }

    public String getNamespaceUri()
    {
        return namespaceUri;
    }

    public boolean equalsByScopeConfig(ComponentDefinitionModel that)
    {
        if (!this.isScope())
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("This method is only supported for scopes"));
        }
        if (this == that) return true;
        if (that == null) return false;

        if (root != that.root) return false;
        if (!namespace.equals(that.namespace)) return false;
        if (!identifier.equals(that.identifier)) return false;
        if (!attributes.equals(that.attributes)) return false;
        if (this.isScope() && that.isScope())
        {
            Stream<ComponentDefinitionModel> thisMps = innerComponents.stream().filter(component -> {
                return !MessageProcessor.class.isAssignableFrom(component.getType());
            });
            thisMps.collect(Collectors.toList());
            Stream<ComponentDefinitionModel> thatMps = that.innerComponents.stream().filter(component -> {
                return !MessageProcessor.class.isAssignableFrom(component.getType());
            });
            thatMps.collect(Collectors.toList());
            return thisMps.equals(thatMps);
        }
        else
        {
            return innerComponents.equals(that.innerComponents);
        }
    }

    public String getTextContent()
    {
        return textContent;
    }

    public static class Builder
    {

        private ComponentDefinitionModel model = new ComponentDefinitionModel();

        public Builder setNamespace(String namespace) {
            //TODO fix this
            if (namespace == null)
            {
                return this;
            }
            this.model.namespace = namespace;
            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.model.identifier = identifier;
            return this;
        }

        public Builder addAttribute(String key, String value) {
            this.model.attributes.put(key, value);
            return this;
        }

        public Builder addChildComponentDefinitionModel(ComponentDefinitionModel componentDefinitionModel) {
            this.model.innerComponents.add(componentDefinitionModel);
            return this;
        }

        public Builder setTextContent(String textContent)
        {
            this.model.textContent = textContent;
            return this;
        }

        public Builder markAsRootComponent()
        {
            this.model.root = true;
            return this;
        }

        public ComponentDefinitionModel build()
        {
            return model;
        }

        public Builder addNameAttribute(String flowName)
        {
            this.model.attributes.put("name", flowName);
            return this;
        }

        public Builder setNode(Node node)
        {
            model.node = node;
            return this;
        }

        //TODO remove this field. Ideally this is the model is XML agnostic and this should in a general purpose map with custom properties.
        public Builder setNamespaceUri(String namespaceUri)
        {
            model.namespaceUri = namespaceUri;
            return this;
        }
    }

    public Builder builderCopy()
    {
        List<ComponentDefinitionModel> childrenCopies = copy(innerComponents);
        Builder builder = new Builder();
        builder.setIdentifier(this.identifier).setNamespace(this.namespace);
        for (ComponentDefinitionModel childrenCopy : childrenCopies)
        {
            builder.addChildComponentDefinitionModel(childrenCopy);
        }
        for (Map.Entry<String, String> entry : attributes.entrySet())
        {
            builder.addAttribute(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    /**
     * deep copy
     * @return
     */
    public ComponentDefinitionModel copy()
    {
        return builderCopy().build();
    }

    private List<ComponentDefinitionModel> copy(List<ComponentDefinitionModel> componentDefinitionModels)
    {
        List<ComponentDefinitionModel> copies = new ArrayList<>();
        for (ComponentDefinitionModel componentDefinitionModel : componentDefinitionModels)
        {
            copies.add(componentDefinitionModel.copy());
        }
        return copies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentDefinitionModel that = (ComponentDefinitionModel) o;

        if (root != that.root) return false;
        if (!namespace.equals(that.namespace)) return false;
        if (!identifier.equals(that.identifier)) return false;
        if (!attributes.equals(that.attributes)) return false;
        return innerComponents.equals(that.innerComponents);

    }

    /**
     * Used to determine if a top level config component is the same as
     * another by name
     * @param o another model
     * @return true if both models have the same name
     * //TODO shouldn't this also compare by the identifier and namespace?
     */
    public int equalsById(Object o) {
        if (this == o) return -1;
        if (o == null || getClass() != o.getClass()) return -1;

        ComponentDefinitionModel that = (ComponentDefinitionModel) o;

        return attributes.get("name").compareTo(that.attributes.get("name"));

    }

    @Override
    public int hashCode() {
        int result = (root ? 1 : 0);
        result = 31 * result + namespace.hashCode();
        result = 31 * result + identifier.hashCode();
        result = 31 * result + attributes.hashCode();
        result = 31 * result + innerComponents.hashCode();
        return result;
    }

}
