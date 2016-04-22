/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_CLASSLOADER;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class ExtensionManifestGeneratorTestCase extends AbstractMuleTestCase
{

    private ExtensionManifestGenerator generator = new ExtensionManifestGenerator();

    private ExtensionModel extensionModel;

    @Before
    public void before() {
        Describer describer = new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver(getProductVersion()));
        ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());
        final DescribingContext context = new DefaultDescribingContext();
        context.addParameter(EXTENSION_CLASSLOADER, getClass().getClassLoader());

        extensionModel = extensionFactory.createFrom(describer.describe(context), context);
    }

    @Test
    public void test() throws Exception {
        Optional<GeneratedResource> resource = generator.generateResource(extensionModel);
        System.out.println(new String(resource.get().getContent()));
    }
}
