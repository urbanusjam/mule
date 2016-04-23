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
import org.mule.runtime.core.config.model.ComponentIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.w3c.dom.Element;

public class ApplicationModel
{
    private static Set<ComponentIdentifier> ignoreNameValidationComponentList = new HashSet<>();
    private List<ComponentDefinitionModel> componentDefinitionModels = new ArrayList<>();

    static {
        ignoreNameValidationComponentList.add(new ComponentIdentifier("mule","flow-ref"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("mule", "alias"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("mule", "in-memory-store"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("mule", "custom-security-provider"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("test","queue"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("db", "data-type"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("db", "in-param"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("db", "out-param"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("db", "template-query-ref"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("db", "inout-param"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("jaas", "password-encryption-strategy"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("jaas", "security-provider"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("spring", "property"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("ss", "user"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("mule-ss", "delegate-security-provider"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("spring", "bean"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("mulexml", "xslt-transformer"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("mulexml", "alias"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("pgp", "security-provider"));
        ignoreNameValidationComponentList.add(new ComponentIdentifier("pgp", "keybased-encryption-strategy"));
    }

    public ApplicationModel(ApplicationConfig applicationConfig) throws Exception {
        List<ConfigFile> configFiles = applicationConfig.getConfigFiles();
        configFiles.stream().filter(configFile -> {
            return !configFile.getConfigLines().get(0).getOperation().equals("beans");
        }).forEach(configFile -> {
            componentDefinitionModels.addAll(extractComponentDefinitionModel(Arrays.asList(configFile.getConfigLines().get(0))));
        });
        validateModel();
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
                if (component.getAttributes().get("when") != null
                    && !component.getNode().getParentNode().getLocalName().equals("choice-exception-strategy")
                        && !component.getNode().getParentNode().getLocalName().equals("mule"))
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
            topLevelComponents.stream().filter(this::isMuleComponent).forEach(topLevelComponent -> {
                topLevelComponent.getInnerComponents().stream().filter(this::isMuleComponent).forEach((topLevelComponentChild -> {
                    executeOnComponentTree(topLevelComponentChild, (component) -> {
                        if (component.getNameAttribute() != null && !ignoreNameValidationComponentList.contains(new ComponentIdentifier(component.getNamespace(), component.getIdentifier())))
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

    private boolean isMuleComponent(ComponentDefinitionModel componentDefinitionModel)
    {
        return !componentDefinitionModel.getIdentifier().equals("beans");
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
        if (component.getNamespace().equals("spring"))
        {
            //TODO for now do no process beans inside spring
            return;
        }
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
