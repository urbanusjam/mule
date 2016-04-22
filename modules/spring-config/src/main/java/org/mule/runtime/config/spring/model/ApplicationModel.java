/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.w3c.dom.Element;

public class ApplicationModel
{

    private List<ComponentDefinitionModel> componentDefinitionModels = new ArrayList<>();

    public ApplicationModel(ApplicationConfig applicationConfig) throws Exception {
        List<ConfigFile> configFiles = applicationConfig.getConfigFiles();
        configFiles.stream().filter(configFile -> {
            return !configFile.getConfigLines().get(0).getOperation().equals("beans");
        }).forEach(configFile -> {
            componentDefinitionModels.addAll(extractComponentDefinitionModel(Arrays.asList(configFile.getConfigLines().get(0))));
        });
        //validateModel();
    }

    private void validateModel() throws ConfigurationException
    {
        //TODO improve performance by processing the tree only once if necessary
        if (componentDefinitionModels.isEmpty() || !componentDefinitionModels.get(0).getIdentifier().equals("mule"))
        {
            return;
        }
        validateNameIsOnlyOnTopElements();
        validateExceptionStrategyWhenAttributeIsOnlyPresentInsideChoice();
        validateChoiceExceptionStrategyStructure();
    }

    private void validateChoiceExceptionStrategyStructure()
    {
        executeOnEveryComponentTree(component -> {
            if (component.getIdentifier().equals("choice-exception-strategy"))
            {
                validateExceptionStrategiesHaveWhenAttribute(component);
                validateNoMoreThanOneRollbackExceptionStrategyWithRedelivery(component);
            }
        });
    }

    private void validateNoMoreThanOneRollbackExceptionStrategyWithRedelivery(ComponentDefinitionModel component)
    {
        if (component.getInnerComponents().stream().filter( exceptionStrategyComponent -> {
            return exceptionStrategyComponent.getAttributes().get("maxRedeliveryAttempts") != null;
        }).count() > 1) {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Only one rollback-exception-strategy within a choice-exception-strategy can handle message redelivery. Remove one of the maxRedeliveryAttempts attributes"));
        }
    }

    private void validateExceptionStrategiesHaveWhenAttribute(ComponentDefinitionModel component)
    {
        List<ComponentDefinitionModel> innerComponents = component.getInnerComponents();
        for (int i = 0; i < innerComponents.size() - 1; i++)
        {
            if (innerComponents.get(i).getAttributes().get("when") == null)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("Every exception strategy (except for the last one) within a choice-exception-strategy must specify the when attribute"));
            }
        }
    }

    private void validateExceptionStrategyWhenAttributeIsOnlyPresentInsideChoice()
    {
        executeOnEveryComponentTree(component -> {
            if (component.getIdentifier().endsWith("exception-strategy"))
            {
                if (component.getAttributes().get("when") != null && !component.getNode().getParentNode().getLocalName().equals("choice-exception-strategy"))
                {
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("Only exception strategies within a choice-exception-strategy can have when attribute specified"));
                }
            }
        });
    }

    private void validateNameIsOnlyOnTopElements() throws ConfigurationException
    {
        try
        {
            List<ComponentDefinitionModel> topLevelComponents = componentDefinitionModels.get(0).getInnerComponents();
            topLevelComponents.forEach(topLevelComponent -> {
                topLevelComponent.getInnerComponents().stream().filter(topLevelComponentChild -> {
                    return !topLevelComponentChild.getIdentifier().equals("beans");
                }).forEach((topLevelComponentChild -> {
                    executeOnComponentTree(topLevelComponentChild, (component) -> {
                        if (component.getNameAttribute() != null && !component.getIdentifier().equals("flow-ref"))
                        {
                            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Only top level elements can have a name attribute. Component %s has attribute name with value %s", getComponentIdentifier(component), component.getNameAttribute()));
                        }
                    });
                }));

            });
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }

    private String getComponentIdentifier(ComponentDefinitionModel component)
    {
        if (component.getNamespace().equals("mule"))
        {
            return component.getIdentifier();
        }
        return component.getNamespace() + ":" + component.getIdentifier();
    }

    private void executeOnEveryComponentTree(final ComponentConsumer task)
    {
        for (ComponentDefinitionModel componentDefinitionModel : componentDefinitionModels)
        {
            executeOnComponentTree(componentDefinitionModel, task);
        }
    }

    private void executeOnComponentTree(final ComponentDefinitionModel component, final ComponentConsumer task) throws MuleRuntimeException
    {
        component.getInnerComponents().forEach((innerComponent) -> {
            executeOnComponentTree(innerComponent, task);
        });
        task.consume(component);
    }

    private List<ComponentDefinitionModel> extractComponentDefinitionModel(List<ConfigLine> configLines)
    {
        List<ComponentDefinitionModel> models = new ArrayList<>();
        for (ConfigLine configLine : configLines) {
            ComponentDefinitionModel.Builder builder = new ComponentDefinitionModel.Builder();
            builder.setNamespace(configLine.getNamespace()).setIdentifier(configLine.getOperation()).setNode(configLine.getNode()).setNamespaceUri(configLine.getNamespaceUri());
            builder.setTextContent(configLine.getTextContent());
            for (ConfigAttribute configAttribute : configLine.getRawAttributes().values()) {
                builder.addAttribute(configAttribute.getName(), configAttribute.getValue());
            }
            List<ComponentDefinitionModel> componentDefinitionModels = extractComponentDefinitionModel(configLine.getChildren());
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
            ConfigLine parent = configLine.getParent();
            if (parent != null && parent.getOperation().equals("mule"))
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

    interface ComponentConsumer {
        void consume(ComponentDefinitionModel componentDefinitionModel) throws MuleRuntimeException;
    }
}
