/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApplicationConfig
{

    private String appName;
    private List<ConfigFile> configFiles = new ArrayList<>();

    private ApplicationConfig()
    {}

    public String getAppName() {
        return appName;
    }

    public List<ConfigFile> getConfigFiles() {
        return Collections.unmodifiableList(configFiles);
    }

    public static class Builder {
        private ApplicationConfig applicationConfig = new ApplicationConfig();

        public Builder setAppName(String appName) {
            this.applicationConfig.appName = appName;
            return this;
        }

        public Builder addConfigFile(ConfigFile configFile) {
            this.applicationConfig.configFiles.add(configFile);
            return this;
        }

        public ApplicationConfig build() {
            return this.applicationConfig;
        }

    }
}
