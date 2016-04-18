/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.extension;

import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

public class LocationKey
{
    @MetadataKeyPart(order = 1)
    private final String continent;

    @MetadataKeyPart(order = 2)
    private final String country;

    @DisplayName("State | City")
    @MetadataKeyPart(order = 3)
    private final String city;

    public LocationKey(String continent, String country, String state)
    {
        this.continent = continent;
        this.country = country;
        this.city = state;
    }

    public String getContinent()
    {
        return continent;
    }

    public String getCountry()
    {
        return country;
    }

    public String getCity()
    {
        return city;
    }
}
