/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.model;

/**
 *
 */
public class ComponentIdentifier
{

    private String namespace;
    private String identifier;

    public ComponentIdentifier(String namespace, String identifier) {
        this.namespace = namespace;
        this.identifier = identifier;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentIdentifier that = (ComponentIdentifier) o;

        if (!namespace.equals(that.namespace)) return false;
        return identifier.equals(that.identifier);

    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + identifier.hashCode();
        return result;
    }
}
