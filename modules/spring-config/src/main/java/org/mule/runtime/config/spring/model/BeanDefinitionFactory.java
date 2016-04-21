/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.runtime.config.spring.util.ProcessingStrategyUtils;
import org.mule.runtime.core.api.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.config.model.AbstractParameterDefinitionVisitor;
import org.mule.runtime.core.config.model.ComponentBuildingDefinition;
import org.mule.runtime.core.config.model.ParameterDefinition;
import org.mule.runtime.core.config.model.TypeDefinitionBuilderVisitor;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.ExtensionManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BeanDefinitionFactory
{
    public static final String ATTRIBUTE_NAME = "name";

    private Logger logger = LoggerFactory.getLogger(BeanDefinitionFactory.class);

    private ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;

    public BeanDefinitionFactory(ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry)
    {
        this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
    }

    public BeanDefinition resolveComponentGoingThroughChildrenUsingParsers(ComponentDefinitionModel componentDefinitionModel, Resource resource, BeanDefinitionRegistry registry, BiConsumer<ComponentDefinitionModel, BeanDefinitionRegistry> resolvedComponentDefinitionModelProcessor, BiFunction<Element, BeanDefinition, BeanDefinition> oldParsingMechansim) {
        List<ComponentDefinitionModel> innerComponents = componentDefinitionModel.getInnerComponents();
        if (!innerComponents.isEmpty())
        {
            for (ComponentDefinitionModel innerComponent : innerComponents) {
                if (hasDefinition(innerComponent.getNamespace(), innerComponent.getIdentifier()))
                {
                    resolveComponentGoingThroughChildrenUsingParsers(innerComponent, resource, registry, resolvedComponentDefinitionModelProcessor, oldParsingMechansim);
                }
                else
                {
                    System.out.println("Resolving element " + innerComponent.getNamespace() + ":" + innerComponent.getIdentifier() + " - OLD MODE");
                    //we can't go to the end. We need to start from he partn
                    BeanDefinition oldBeanDefinition = oldParsingMechansim.apply((Element) innerComponent.getNode(), null);
                    oldBeanDefinition = wrapBeanDefinitionForFilters(componentDefinitionModel.getNode(), oldBeanDefinition);
                    innerComponent.setBeanDefinition(oldBeanDefinition);
                }
            }
            //Process, all childs resolved
        }
        //Logic to process since is
        return resolveComponentUsingParsers(componentDefinitionModel, resource, registry, resolvedComponentDefinitionModelProcessor, oldParsingMechansim);
    }

    public static BeanDefinition wrapBeanDefinitionForFilters(Node parentNode, BeanDefinition oldBeanDefinition)
    {
        if (oldBeanDefinition == null)
        {
            return null;
        }
        //TODO improve this condition with constatns
        if (parentNode.getNodeName().contains("message-filter") || parentNode.getNodeName().contains("mule") || parentNode.getNodeName().endsWith("-filter"))
        {
            return oldBeanDefinition;
        }
        Class beanClass;
        if (oldBeanDefinition instanceof RootBeanDefinition)
        {
            beanClass = ((RootBeanDefinition) oldBeanDefinition).getBeanClass();
        }
        else
        {
            if (oldBeanDefinition.getBeanClassName() == null)
            {
                return oldBeanDefinition;
            }
            try
            {
                beanClass = Class.forName(oldBeanDefinition.getBeanClassName());
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
        if (areMatchingTypes(Filter.class, beanClass))
        {
            boolean failOnUnaccepted = false;
            Object processorWhenUnaccepted = null;
            oldBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(MessageFilter.class)
                .addConstructorArgValue(oldBeanDefinition)
                .addConstructorArgValue(failOnUnaccepted)
                .addConstructorArgValue(processorWhenUnaccepted)
                .getBeanDefinition();
        }
        else if (areMatchingTypes(SecurityFilter.class, beanClass))
        {
            oldBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SecurityFilterMessageProcessor.class)
                    .addPropertyValue("filter", oldBeanDefinition)
                    .getBeanDefinition();
        }
        return oldBeanDefinition;
    }


    public BeanDefinition resolveComponentUsingParsers(ComponentDefinitionModel componentDefinitionModel, Resource resource, BeanDefinitionRegistry registry, BiConsumer<ComponentDefinitionModel, BeanDefinitionRegistry> componentDefinitionModelProcessor, BiFunction<Element, BeanDefinition, BeanDefinition> oldParsingMechansim) {
        if (componentDefinitionModel.getIdentifier().equals("mule"))
        {
            return null;
        }
        resolveComponentBeanDefinitionUsingParsers(componentDefinitionModel, resource, oldParsingMechansim);
        componentDefinitionModelProcessor.accept(componentDefinitionModel, registry);
        return componentDefinitionModel.getBeanDefinition();
        //TODO add throw if not present
    }

    public void resolveComponentBeanDefinitionUsingParsers(ComponentDefinitionModel componentDefinitionModel, Resource resource, BiFunction<Element, BeanDefinition, BeanDefinition> oldParsingMechansim) {
        ComponentBuildingDefinition componentBuildingDefinition = componentBuildingDefinitionRegistry.getBuildingDefinition(componentDefinitionModel.getNamespace(), componentDefinitionModel.getIdentifier());

        if (componentDefinitionModel.getNamespace().equals("spring") || componentDefinitionModel.getNamespace().equals("context"))
        {
            //TODO add a more reliable way to avoid processing elements that are not defined by mule
            return;
        }

        if (componentBuildingDefinition == null)
        {
            //Parse using old method
            //throw new RuntimeException("No builder definition for: " + componentDefinitionModel.getNamespace() + ":" + componentDefinitionModel.getIdentifier());
            System.out.println("Resolving element " + componentDefinitionModel.getNamespace() + ":" + componentDefinitionModel.getIdentifier() + " - OLD MODE");
            BeanDefinition beanDefinition = oldParsingMechansim.apply((Element) componentDefinitionModel.getNode(), null);
            beanDefinition = wrapBeanDefinitionForFilters(componentDefinitionModel.getNode().getParentNode(), beanDefinition);
            componentDefinitionModel.setBeanDefinition(beanDefinition);
            return;
        }


        System.out.println("Resolving element " + componentDefinitionModel.getNamespace() + ":" + componentDefinitionModel.getIdentifier() + " - NEW MODE");

        final AtomicReference<Class<?>> typeReference = new AtomicReference<>();
        componentBuildingDefinition.getTypeDefinitionBuilder().visit(new TypeDefinitionBuilderVisitor()
        {
            @Override
            public void onType(Class<?> type)
            {
                typeReference.set(type);
            }

            @Override
            public void onConfigurationAttribute(String attributeName)
            {
                try
                {
                    typeReference.set(Class.forName(componentDefinitionModel.getAttributes().get(attributeName)));
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeException("Error while trying to locate Class definition for type " + attributeName, e);
                }
            }
        });
        componentDefinitionModel.setType(typeReference.get());
        BeanDefinitionBuilder beanDefinitionBuilder;
        if (componentBuildingDefinition.getObjectFactoryType() != null)
        {
            Class<?> objectFactoryType = componentBuildingDefinition.getObjectFactoryType();
            Enhancer enhancer = new Enhancer();
            enhancer.setInterfaces(new Class[]{FactoryBean.class});
            enhancer.setSuperclass(objectFactoryType);
            enhancer.setCallbackType(MethodInterceptor.class);
            Class factoryBeanClass = enhancer.createClass();
            Enhancer.registerStaticCallbacks(factoryBeanClass, new Callback[]{
                new MethodInterceptor()
                {
                    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable
                    {
                        if (method.getName().equals("isSingleton"))
                        {
                            return !componentBuildingDefinition.isPrototype();
                        }
                        return proxy.invokeSuper(obj, args);
                    }
                }
            });
            beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanClass);
        }
        else
        {
            beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(typeReference.get());
        }

        processAnnotations(componentDefinitionModel, resource, beanDefinitionBuilder);

        List<ParameterDefinition> parameterDefinitions = componentBuildingDefinition.getConstructorParameterDefinition();
        if (!parameterDefinitions.isEmpty() || !componentBuildingDefinition.getSetterParameterDefinitions().isEmpty())
        {
            Map<String, String> undefinedSimpleParameters = new HashMap(componentDefinitionModel.getAttributes());
            List<BeanDefinitionTypePair> undefinedComplexParameters = componentDefinitionModel.getInnerComponents().stream().map(cdm -> {
                //When it comes from old model it does not have the type set
                Class<?> type = cdm.getType();
                if (type == null)
                {
                    if (cdm.getBeanDefinition() == null)
                    {
                        //TODO see what to do here.
                        type = Object.class;
                    }
                    else
                    {
                        try
                        {
                            type = Class.forName(cdm.getBeanDefinition().getBeanClassName());
                        }
                        catch (ClassNotFoundException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
                return new BeanDefinitionTypePair(type, cdm.getBeanDefinition());
            }).collect(Collectors.toList());

            componentBuildingDefinition.getSetterParameterDefinitions().entrySet().stream().forEach(parameterDefinitionEntry -> {
                parameterDefinitionEntry.getValue().accept(new AbstractParameterDefinitionVisitor()
                {
                    @Override
                    public void onReferenceObject(Class<?> objectType)
                    {
                        if (MuleContext.class.equals(objectType))
                        {
                            beanDefinitionBuilder.addPropertyReference(parameterDefinitionEntry.getKey(), OBJECT_MULE_CONTEXT);
                        }
                        else if (TimeSupplier.class.equals(objectType))
                        {
                            beanDefinitionBuilder.addPropertyReference(parameterDefinitionEntry.getKey(), OBJECT_TIME_SUPPLIER);
                        }
                        else if (ExtensionManager.class.equals(objectType))
                        {
                            beanDefinitionBuilder.addPropertyReference(parameterDefinitionEntry.getKey(), OBJECT_EXTENSION_MANAGER);
                        }
                        else
                        {
                            throw new RuntimeException();
                        }
                    }

                    @Override
                    public void onProvidedValue(Object defaultValue)
                    {
                        beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(), defaultValue);
                    }

                    @Override
                    public void onConfigurationParameter(String parameterName, Object defaultValue)
                    {
                        String parameterValue = undefinedSimpleParameters.get(parameterName);
                        Object value = parameterValue != null ? parameterValue : defaultValue;
                        if (value != null)
                        {
                            undefinedSimpleParameters.remove(parameterName);
                            beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(), value);
                        }
                    }

                    @Override
                    public void onReferenceSimpleParameter(String parameterName)
                    {
                        String reference = undefinedSimpleParameters.get(parameterName);
                        if (parameterName.equals("processingStrategy"))
                        {
                            ProcessingStrategy processingStrategy = ProcessingStrategyUtils.parseProcessingStrategy(reference);
                            if (processingStrategy != null)
                            {
                                beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(), processingStrategy);
                                return;
                            }
                        }
                        if (reference != null)
                        {
                            undefinedSimpleParameters.remove(reference);
                            beanDefinitionBuilder.addPropertyReference(parameterDefinitionEntry.getKey(), reference);
                        }
                    }

                    @Override
                    public void onUndefinedSimpleParameters()
                    {
                        beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(), undefinedSimpleParameters);
                    }

                    @Override
                    public void onUndefinedComplexParameters()
                    {
                        beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(),
                            constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(undefinedComplexParameters)));
                    }

                    @Override
                    public void onComplexChildList(Class<?> type)
                    {
                        List<BeanDefinitionTypePair> matchingBeanDefinitionTypePairs = undefinedComplexParameters.stream().filter(beanDefinitionTypePair -> {
                            //TODO this code is used elsewhere - refactor to improve
                            if (areMatchingTypes(FactoryBean.class, beanDefinitionTypePair.getType()))
                            {
                                try
                                {
                                    return areMatchingTypes(type, ((FactoryBean) beanDefinitionTypePair.getType().newInstance()).getObjectType());
                                }
                                catch (Exception e)
                                {
                                    throw new RuntimeException(e);
                                }
                            }
                            return areMatchingTypes(type, beanDefinitionTypePair.getType());
                        }).collect(Collectors.toList());

                        matchingBeanDefinitionTypePairs.stream().forEach(beanDefinitionTypePair -> {
                            undefinedComplexParameters.remove(beanDefinitionTypePair);
                        });

                        beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(),
                            constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(matchingBeanDefinitionTypePairs)));
                    }

                    @Override
                    public void onComplexChild(Class<?> type)
                    {
                        Optional<BeanDefinitionTypePair> value = undefinedComplexParameters.stream().filter(beanDefinitionTypePair -> {
                            return areMatchingTypes(type, beanDefinitionTypePair.getType());
                        }).findFirst();
                        value.ifPresent(beanDefinitionTypePair -> {
                            undefinedComplexParameters.remove(beanDefinitionTypePair);
                            beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(), beanDefinitionTypePair.getBeanDefinition());
                        });
                    }

                    @Override
                    public void onValueFromTextContent()
                    {
                        beanDefinitionBuilder.addPropertyValue(parameterDefinitionEntry.getKey(), componentDefinitionModel.getTextContent());
                    }
                });
            });

            parameterDefinitions.stream().forEach(constructorParameterDefinition -> {
                constructorParameterDefinition.accept(new AbstractParameterDefinitionVisitor()
                {
                    @Override
                    public void onReferenceObject(Class<?> objectType)
                    {
                        if (MuleContext.class.equals(objectType))
                        {
                            beanDefinitionBuilder.addConstructorArgReference(OBJECT_MULE_CONTEXT);
                        }
                        else if (TimeSupplier.class.equals(objectType))
                        {
                            beanDefinitionBuilder.addConstructorArgReference(OBJECT_TIME_SUPPLIER);
                        }
                        else if (ExtensionManager.class.equals(objectType))
                        {
                            beanDefinitionBuilder.addConstructorArgReference(OBJECT_EXTENSION_MANAGER);
                        }
                        else
                        {
                            throw new RuntimeException();
                        }
                    }

                    @Override
                    public void onProvidedValue(Object value)
                    {
                        beanDefinitionBuilder.addConstructorArgValue(value);
                    }

                    @Override
                    public void onConfigurationParameter(String parameterName, Object defaultValue)
                    {
                        Object value = undefinedSimpleParameters.get(parameterName);
                        undefinedSimpleParameters.remove(parameterName);
                        beanDefinitionBuilder.addConstructorArgValue(Optional.ofNullable(value).orElse(defaultValue));
                    }

                    @Override
                    public void onReferenceSimpleParameter(String parameterName)
                    {
                        String reference = undefinedSimpleParameters.get(parameterName);
                        if (parameterName.equals("processingStrategy"))
                        {
                            ProcessingStrategy processingStrategy = ProcessingStrategyUtils.parseProcessingStrategy(reference);
                            if (processingStrategy != null)
                            {
                                beanDefinitionBuilder.addConstructorArgValue(processingStrategy);
                                return;
                            }
                        }
                        undefinedSimpleParameters.remove(parameterName);
                        if (reference != null)
                        {
                            beanDefinitionBuilder.addConstructorArgReference(reference);
                        }
                        else
                        {
                            beanDefinitionBuilder.addConstructorArgValue(null);
                        }
                    }

                    @Override
                    public void onUndefinedSimpleParameters()
                    {
                        beanDefinitionBuilder.addConstructorArgValue(undefinedSimpleParameters);
                    }

                    @Override
                    public void onUndefinedComplexParameters()
                    {
                        beanDefinitionBuilder.addConstructorArgValue(constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(undefinedComplexParameters)));
                    }

                    @Override
                    public void onComplexChildList(Class<?> type)
                    {
                        List<BeanDefinitionTypePair> matchingBeanDefinitionTypePairs = undefinedComplexParameters.stream().filter(beanDefinitionTypePair -> {
                            return areMatchingTypes(type, beanDefinitionTypePair.getType());
                        }).collect(Collectors.toList());

                        matchingBeanDefinitionTypePairs.stream().forEach(beanDefinitionTypePair -> {
                            undefinedComplexParameters.remove(beanDefinitionTypePair);
                        });

                        beanDefinitionBuilder.addConstructorArgValue(
                            constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(matchingBeanDefinitionTypePairs)));
                    }

                    @Override
                    public void onComplexChild(Class<?> type)
                    {
                        //TODO refactor to reuse code from the other complex child processing
                        Optional<BeanDefinitionTypePair> value = undefinedComplexParameters.stream().filter(beanDefinitionTypePair -> {
                            return areMatchingTypes(type, beanDefinitionTypePair.getType());
                        }).findFirst();
                        value.ifPresent(beanDefinitionTypePair -> {
                            undefinedComplexParameters.remove(beanDefinitionTypePair);
                            beanDefinitionBuilder.addConstructorArgValue(beanDefinitionTypePair.getBeanDefinition());
                        });
                    }
                });
            });
        }

        //TODO This forces a setMuleContext over the FactoryBean when the one having the mule context is the object
        //addMuleContextIfBeanIsMuleContextAware(typeReference.get(), beanDefinitionBuilder);

//        addInitialiseMethodToBeanDefinitionBuilder(typeReference.get(), beanDefinitionBuilder); - We need to do initialization manually because if not spring won't allow us to inject the FlowConstruct before the inner component initialization
        beanDefinitionBuilder.setLazyInit(true);
        if (componentBuildingDefinition.isPrototype())
        {
            beanDefinitionBuilder.setScope("prototype");
        }

//        if (componentBuildingDefinition.isScope())
//        {
//            List<BeanDefinition> messageProcessorsBeanDefinitions = componentDefinitionModel.getInnerComponents().stream().filter(model -> {
//                return areMatchingTypes(MessageProcessor.class, model.getType());
//            }).map(messageProcessorModel -> {
//                return messageProcessorModel.getBeanDefinition();
//            }).collect(Collectors.toList());
//            BeanDefinitionBuilder scopeBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ScopeMessageProcessor.class);
//            scopeBeanDefinitionBuilder.addConstructorArgValue(beanDefinitionBuilder.getBeanDefinition());
//            ManagedList<Object> innerMessageProcessors = new ManagedList<>();
//            innerMessageProcessors.addAll(messageProcessorsBeanDefinitions);
//            scopeBeanDefinitionBuilder.addConstructorArgValue(innerMessageProcessors);
//            addInitialiseMethodToBeanDefinitionBuilder(ScopeMessageProcessor.class, scopeBeanDefinitionBuilder);
//            scopeBeanDefinitionBuilder.setLazyInit(true);
//            componentDefinitionModel.setBeanDefinition(scopeBeanDefinitionBuilder.getBeanDefinition());
//        }
//        else
//        {
            componentDefinitionModel.setBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
//        }
    }

    private void processAnnotations(ComponentDefinitionModel componentDefinitionModel, Resource resource, BeanDefinitionBuilder beanDefinitionBuilder)
    {
        //TODO join this two methods one we get rid of AbstractMuleBeanDefinitionParser.processMetadataAnnotationsHelper
        Map<QName, Object> annotations = AbstractMuleBeanDefinitionParser.processMetadataAnnotationsHelper((Element) componentDefinitionModel.getNode(), resource, beanDefinitionBuilder);
        processAnnotationParameters(componentDefinitionModel, annotations);
        processNestedAnnotations(componentDefinitionModel, annotations);
        if (!annotations.isEmpty())
        {
            beanDefinitionBuilder.addPropertyValue(AnnotatedObject.PROPERTY_NAME, annotations);
        }
    }

    private void processAnnotationParameters(ComponentDefinitionModel componentDefinitionModel, Map<QName, Object> annotations)
    {
        componentDefinitionModel.getAttributes().entrySet().stream().filter( entry -> {
            return entry.getKey().contains(":");
        }).forEach( annotationKey -> {
            Node attribute = componentDefinitionModel.getNode().getAttributes().getNamedItem(annotationKey.getKey());
            if (attribute != null)
            {
                annotations.put(new QName(attribute.getNamespaceURI(), attribute.getLocalName()), annotationKey.getValue());
            }
        });
    }

    public static void checkElementNameUnique(BeanDefinitionRegistry registry, Element element)
    {
        if (null != element.getAttributeNode(ATTRIBUTE_NAME))
        {
            String name = element.getAttribute(ATTRIBUTE_NAME);
            if (registry.containsBeanDefinition(name))
            {
                throw new IllegalArgumentException("A service named " + name + " already exists.");
            }
        }
    }

    private void processNestedAnnotations(ComponentDefinitionModel componentDefinitionModel, Map<QName, Object> previousAnnotations)
    {
        componentDefinitionModel.getInnerComponents().stream().filter( cdm -> {
            return cdm.getIdentifier().equals("annotations") && cdm.getNamespace().equals("mule");
        }).findFirst().ifPresent(annotationsCdm -> {

            annotationsCdm.getInnerComponents().forEach(annotationCdm -> {
                //TODO use right namespace from doc
                previousAnnotations.put(new QName(annotationCdm.getNamespaceUri(), annotationCdm.getIdentifier()), annotationCdm.getTextContent());
            }
            );
            //for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
            //{
            //    switch (child.getNodeType())
            //    {
            //        case Node.TEXT_NODE:
            //        case Node.CDATA_SECTION_NODE:
            //            builder.append(child.getNodeValue());
            //    }
            //}
            //assembler.addAnnotationValue(context.getContainingBeanDefinition().getPropertyValues(),
            //                             new QName(node.getNamespaceURI(), node.getLocalName()),
            //                             builder.toString());
        });
    }


    private List<BeanDefinition> fromBeanDefinitionTypePairToBeanDefinition(List<BeanDefinitionTypePair> undefinedComplexParameters)
    {
        return undefinedComplexParameters.stream().map(beanDefinitionTypePair -> {
            return beanDefinitionTypePair.getBeanDefinition();
        }).collect(Collectors.toList());
    }

    private ManagedList constructManagedList(List<BeanDefinition> beanDefinitions)
    {
        ManagedList managedList = new ManagedList();
        managedList.addAll(beanDefinitions);
        return managedList;
    }

    private ManagedList constructManagedList(Map<Class, BeanDefinition> undefineidComplexParameters)
    {
        ManagedList complexParameters = new ManagedList();
        undefineidComplexParameters.entrySet().forEach(undefinedComplexParameterEntry -> {
            complexParameters.add(undefinedComplexParameterEntry.getValue());
        });
        return complexParameters;
    }

    private void addMuleContextIfBeanIsMuleContextAware(Class<?> componentBuildingDefinitionType, BeanDefinitionBuilder beanDefinitionBuilder)
    {
        if (areMatchingTypes(MuleContextAware.class, componentBuildingDefinitionType))
        {
            new MuleContextSetterAttributeBinder().apply(beanDefinitionBuilder);
        }
    }

    private void addMuleContextIfPresentInConstructor(BeanDefinitionBuilder beanDefinitionBuilder, Constructor<?>[] declaredConstructors)
    {
        if (declaredConstructors.length > 1)
        {
            throw new RuntimeException();
        }
        if (declaredConstructors.length > 0)
        {
            Constructor<?> declaredConstructor = declaredConstructors[0];
            Class<?>[] parameterTypes = declaredConstructor.getParameterTypes();
            for (Class<?> parameterType : parameterTypes)
            {
                if (areMatchingTypes(MuleContext.class, parameterType))
                {
                    new MuleContextConstructorBinder().apply(beanDefinitionBuilder);
                }
            }
        }
    }

    private static boolean areMatchingTypes(Class<?> superType, Class<?> childType)
    {
        return superType.isAssignableFrom(childType);
    }

    private void addInitialiseMethodToBeanDefinitionBuilder(Class<?> type, BeanDefinitionBuilder beanDefinitionBuilder)
    {
        if (areMatchingTypes(Initialisable.class, type))
        {
            beanDefinitionBuilder.setInitMethodName("initialise");
        }
    }

    public boolean hasDefinition(String namespace, String operationName)
    {
        return componentBuildingDefinitionRegistry.getBuildingDefinition(namespace, operationName) != null;
    }

}
