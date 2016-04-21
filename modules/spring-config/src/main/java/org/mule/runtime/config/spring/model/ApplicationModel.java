/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.w3c.dom.Element;

public class ApplicationModel
{

    private List<ComponentDefinitionModel> componentDefinitionModels = new ArrayList<>();

    public ApplicationModel(ApplicationConfig applicationConfig) {
        List<ConfigFile> configFiles = applicationConfig.getConfigFiles();
        configFiles.stream().filter(configFile -> {
            return !configFile.getConfigLines().get(0).getOperation().equals("beans");
        }).forEach(configFile -> {
            //TODO for now let's don't do nothing with "mule"
            componentDefinitionModels.addAll(extractComponentDefinitionModel(configFile.getConfigLines().get(0).getChildren(), true));
        });
    }

    private List<ComponentDefinitionModel> extractComponentDefinitionModel(List<ConfigLine> configLines, boolean isRoot)
    {
        List<ComponentDefinitionModel> models = new ArrayList<>();
        for (ConfigLine configLine : configLines) {
            ComponentDefinitionModel.Builder builder = new ComponentDefinitionModel.Builder();
            builder.setNamespace(configLine.getNamespace()).setIdentifier(configLine.getOperation()).setNode(configLine.getNode()).setNamespaceUri(configLine.getNamespaceUri());
            builder.setTextContent(configLine.getTextContent());
            for (ConfigAttribute configAttribute : configLine.getRawAttributes().values()) {
                builder.addAttribute(configAttribute.getName(), configAttribute.getValue());
            }
            List<ComponentDefinitionModel> componentDefinitionModels = extractComponentDefinitionModel(configLine.getChildren(), false);
            componentDefinitionModels.stream().forEach(componentDefinitionModel -> {
                if (componentDefinitionModel.getIdentifier().equals("property") && componentDefinitionModel.getNamespace().equals("spring"))
                {
                    builder.addAttribute(componentDefinitionModel.getNameAttribute(), componentDefinitionModel.getAttributes().get("value"));
                }
                else
                {
                    builder.addChildComponentDefinitionModel(componentDefinitionModel);
                }
            });
            if (isRoot)
            {
                builder.markAsRootComponent();
            }
            models.add(builder.build());
        }
        return models;
    }

    public void foreach(Consumer<ComponentDefinitionModel> componentDefinitionModelConsumer)
    {
        innerForeach(componentDefinitionModelConsumer, componentDefinitionModels);
    }

    private void innerForeach(Consumer<ComponentDefinitionModel> componentDefinitionModelConsumer, List<ComponentDefinitionModel> componentDefinitionModels)
    {
        for (ComponentDefinitionModel componentDefinitionModel : componentDefinitionModels)
        {
            componentDefinitionModelConsumer.accept(componentDefinitionModel);
            innerForeach(componentDefinitionModelConsumer, componentDefinitionModel.getInnerComponents());
        }
    }

    public List<ComponentDefinitionModel> getComponentDefinitionModels() {
        return componentDefinitionModels;
    }

    public ComponentDefinitionModel getRootComponentModelByName(String componentId)
    {
        for (ComponentDefinitionModel componentDefinitionModel : componentDefinitionModels)
        {
            if (componentDefinitionModel.getNameAttribute().equals(componentId))
            {
                return componentDefinitionModel;
            }
        }
        throw new RuntimeException();
    }

    //TODO remove once the old parsing mechanism is not needed anymore
    public ComponentDefinitionModel findComponentDefinitionModel(Element element)
    {
        return innerFindComponentDefinitionModel(element, componentDefinitionModels);
    }

    private ComponentDefinitionModel innerFindComponentDefinitionModel(Element element, List<ComponentDefinitionModel> componentDefinitionModels)
    {
        for (ComponentDefinitionModel componentDefinitionModel : componentDefinitionModels)
        {
            if (componentDefinitionModel.getNode().equals(element))
            {
                return componentDefinitionModel;
            }
            ComponentDefinitionModel childComponentDefinitionModel = innerFindComponentDefinitionModel(element, componentDefinitionModel.getInnerComponents());
            if (childComponentDefinitionModel != null)
            {
                return childComponentDefinitionModel;
            }
        }
        return null;
    }
}
