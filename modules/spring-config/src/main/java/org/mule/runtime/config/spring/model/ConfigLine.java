/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import org.mule.runtime.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

public class ConfigLine
{

    ConfigLineProvider parent;
    String namespace;
    String operation;

    Map<String, ConfigAttribute> rawAttributes = new HashMap<>();

    List<ConfigLine> childs = new ArrayList<>();
    //TODO remove once we don't need the old parsing mechanism anymore.
    private Node node;
    private String textContent;
    private String namespaceUri;

    public ConfigLine()
    {
    }

    public String getNamespace() {
        return namespace;
    }

    public String getOperation() {
        return operation;
    }

    public Map<String, ConfigAttribute> getRawAttributes() {
        return Collections.unmodifiableMap(rawAttributes);
    }

    public List<ConfigLine> getChildren() {
        return childs;
    }

    public ConfigLine getParent() {
        return parent.getConfigLine();
    }

    public Node getNode()
    {
        return node;
    }

    public String getTextContent()
    {
        return textContent;
    }

    public String getNamespaceUri()
    {
        return namespaceUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigLine that = (ConfigLine) o;

        if (parent != null ? !parent.equals(that.parent) : that.parent != null) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
        if (operation != null ? !operation.equals(that.operation) : that.operation != null) return false;
        if (rawAttributes != null ? !rawAttributes.equals(that.rawAttributes) : that.rawAttributes != null)
            return false;
        return childs != null ? childs.equals(that.childs) : that.childs == null;

    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        result = 31 * result + (rawAttributes != null ? rawAttributes.hashCode() : 0);
        result = 31 * result + (childs != null ? childs.hashCode() : 0);
        return result;
    }

    //TODO remove, this should be immutable
    public void addChild(ConfigLine configLine)
    {
        this.childs.add(configLine);
    }

    public static class ConfigLineBuilder {
        private ConfigLine configLine = new ConfigLine();
        private boolean alreadyBuild;
        private Node node;

        public ConfigLineBuilder setNamespace(String namespace) {
            Preconditions.checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.namespace = namespace;
            return this;
        }

        public ConfigLineBuilder setOperation(String operation) {
            Preconditions.checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.operation = operation;
            return this;
        }

        public ConfigLineBuilder addAttribute(String namespaceUri, String name, String value)
        {
            Preconditions.checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.rawAttributes.put(name, new ConfigAttribute(namespaceUri, name, value));
            return this;
        }

        public ConfigLineBuilder addChild(ConfigLine line)
        {
            Preconditions.checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.childs.add(line);
            return this;
        }

        public ConfigLineBuilder setParent(ConfigLineProvider parent)
        {
            Preconditions.checkState(!alreadyBuild, "builder already build an object, you cannot modify it");
            configLine.parent = parent;
            return this;
        }

        public ConfigLineBuilder setNode(Node node)
        {
            configLine.node = node;
            return this;
        }

        public ConfigLineBuilder setTextContent(String textContent)
        {
            configLine.textContent = textContent;
            return this;
        }

        public ConfigLineBuilder setNamespaceUri(String namespaceUri)
        {
            configLine.namespaceUri = namespaceUri;
            return this;
        }

        public ConfigLine build()
        {
            alreadyBuild = true;
            return configLine;
        }
    }

}
