/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.parameter.ParametrizedModel;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

final class ExportedArtifactsCollector
{

    private final Set<String> filteredPackages = ImmutableSet.<String>builder()
            .add("java.", "javax.", "org.mule.runtime.").build();

    private final ExtensionModel extensionModel;

    ExportedArtifactsCollector(ExtensionModel extensionModel)
    {
        this.extensionModel = extensionModel;
    }

    Set<String> getExportedPackages()
    {
        Set<Class> exportedClasses = classSet();
        exportedClasses.addAll(collectImplementingClasses());
        exportedClasses.addAll(collectParameterClasses());
        exportedClasses.addAll(collectReturnTypes());

        return exportedClasses.stream()
                .filter(type -> type.getPackage() != null)
                .map(type -> type.getPackage().getName())
                .filter(packageName -> filteredPackages.stream().noneMatch(filtered -> packageName.startsWith(filtered)))
                .collect(toSet());
    }

    private Set<Class> collectReturnTypes()
    {
        Set<Class> classes = classSet();
        classes.addAll(collectReturnTypes(extensionModel.getOperationModels(), extensionModel.getSourceModels()));
        extensionModel.getConfigurationModels().forEach(configuration ->
                                                                classes.addAll(collectReturnTypes(configuration.getOperationModels(), configuration.getSourceModels()))
        );

        return classes;
    }

    private Set<Class> collectReturnTypes(Collection<? extends ComponentModel>... componentModelsArray)
    {
        Set<Class> classes = classSet();

        stream(componentModelsArray).forEach(componentList -> componentList.forEach(component -> {
            classes.add(getType(component.getReturnType()));
            classes.add(getType(component.getAttributesType()));
        }));

        return classes;
    }

    private Set<Class> collectParameterClasses()
    {
        Set<Class> classes = classSet();
        classes.addAll(collectParameterClasses(
                extensionModel.getConnectionProviders(),
                extensionModel.getConfigurationModels(),
                extensionModel.getOperationModels(),
                extensionModel.getSourceModels()));

        extensionModel.getConfigurationModels().forEach(configuration ->
                                                                classes.addAll(collectParameterClasses(
                                                                        configuration.getConnectionProviders(),
                                                                        configuration.getOperationModels(),
                                                                        configuration.getSourceModels()))
        );

        return classes;
    }

    private Set<Class> collectParameterClasses(Collection<? extends ParametrizedModel>... parametrizedModelsArray)
    {
        Set<Class> classes = classSet();
        MetadataTypeVisitor visitor = new MetadataTypeVisitor()
        {
            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                dictionaryType.getKeyType().accept(this);
                dictionaryType.getValueType().accept(this);
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                final Class<Object> clazz = getType(objectType);

                classes.add(clazz);
            }
        };

        stream(parametrizedModelsArray).forEach(modelList -> modelList.forEach(
                model -> model.getParameterModels().forEach(p -> p.getType().accept(visitor))));

        return classes;
    }

    private Set<Class> collectImplementingClasses()
    {
        return collectImplementingClasses(collectEnrichableModels());
    }


    private Set<Class> collectImplementingClasses(Collection<EnrichableModel> models)
    {
        return models.stream()
                .map(model -> model.getModelProperty(ImplementingTypeModelProperty.class))
                .map(property -> property.isPresent() ? property.get().getType() : null)
                .filter(property -> property != null)
                .collect(toSet());
    }

    private Collection<EnrichableModel> collectEnrichableModels()
    {
        Set<EnrichableModel> enrichableModels = new HashSet<>();

        enrichableModels.add(extensionModel);
        enrichableModels.addAll(extensionModel.getConfigurationModels());
        enrichableModels.addAll(extensionModel.getOperationModels());
        enrichableModels.addAll(extensionModel.getSourceModels());
        enrichableModels.addAll(extensionModel.getConnectionProviders());
        extensionModel.getConfigurationModels().forEach(configuration -> {
            enrichableModels.addAll(configuration.getOperationModels());
            enrichableModels.addAll(configuration.getOperationModels());
            enrichableModels.addAll(configuration.getSourceModels());
            enrichableModels.addAll(configuration.getConnectionProviders());
        });

        return enrichableModels;
    }

    private HashSet<Class> classSet()
    {
        return new HashSet<>();
    }
}
