/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.model;

import java.util.Optional;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlApplicationElementParser
{
    public Optional<ConfigLine> parse(Element configElement)
    {
        return configLineFromElement(configElement, () -> {
            return null;
        });
    }

    private Optional<ConfigLine> configLineFromElement(Node node, ConfigLineProvider parentProvider) {
        if (node.getNodeName().equals("#text") || node.getNodeName().equals("#comment"))
        {
            return Optional.empty();
        }
        String nodeName = node.getNodeName();
        String[] nodeNameParts = node.getNodeName().split(":");
        String schema = null;
        String elementName = nodeName;

        if (nodeNameParts.length > 1)
        {
            schema = node.getNodeName().split(":")[0];
            elementName = node.getNodeName().split(":")[1];
        }

        ConfigLine.ConfigLineBuilder configLineBuilder = new ConfigLine.ConfigLineBuilder()
            .setOperation(elementName).setNamespace(schema).setNamespaceUri(node.getNamespaceURI()).setNode(node).setParent(parentProvider);
        NamedNodeMap attributes = node.getAttributes();
        if (node.hasAttributes())
        {
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Node attribute = attributes.item(i);
                configLineBuilder.addAttribute(attribute.getNamespaceURI(), attribute.getNodeName(), attribute.getNodeValue());
            }
        }
        if (node.hasChildNodes())
        {
            NodeList childs = node.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++)
            {
                Node child = childs.item(i);
                if (child.getNodeName().equals("#text"))
                {
                    configLineBuilder.setTextContent(child.getNodeValue());
                }
                else
                {
                    configLineFromElement(child, () -> {
                        return configLineBuilder.build();
                    }).ifPresent(configLine -> {
                        configLineBuilder.addChild(configLine);
                    });
                }
            }
        }
        return Optional.of(configLineBuilder.build());
    }
}
