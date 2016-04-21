/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.model;

import java.util.List;

public class ConfigFile implements Comparable<ConfigFile> {

    private String filename;
    private List<ConfigLine> configLines;

    public ConfigFile(String filename, List<ConfigLine> configLines) {
        this.filename = filename;
        this.configLines = configLines;
    }

    public String getFilename() {
        return filename;
    }

    public List<ConfigLine> getConfigLines() {
        return configLines;
    }

    @Override
    public int compareTo(ConfigFile o) {
        return filename.compareTo(o.filename);
    }
}
