/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.project;

import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.function.Supplier;

public abstract class SerializableLightSettings implements DOMConvertable, LightSettings
{
    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightInstance");
        element.setAttribute("targetX", Double.toString(getTargetX()));
        element.setAttribute("targetY", Double.toString(getTargetY()));
        element.setAttribute("targetZ", Double.toString(getTargetZ()));
        element.setAttribute("azimuth", Double.toString(getAzimuth()));
        element.setAttribute("inclination", Double.toString(getInclination()));
        element.setAttribute("log10Distance", Double.toString(getLog10Distance()));
        element.setAttribute("intensity", Double.toString(getIntensity()));
        element.setAttribute("spotSize", Double.toString(getSpotSize()));
        element.setAttribute("spotTaper", Double.toString(getSpotTaper()));
        element.setAttribute("locked", Boolean.toString(isLocked()));
        element.setAttribute("name", getName());
        element.setAttribute("color", getColor().toString());
        return element;
    }

    public static <LightType extends SerializableLightSettings>
    LightType fromDOMElement(Element element, Supplier<LightType> lightInstanceSettingConstructor)
    {
        LightType setting = lightInstanceSettingConstructor.get();
        setting.setName(element.getAttribute("name"));
        setting.setTargetX(Double.parseDouble(element.getAttribute("targetX")));
        setting.setTargetY(Double.parseDouble(element.getAttribute("targetY")));
        setting.setTargetZ(Double.parseDouble(element.getAttribute("targetZ")));
        setting.setAzimuth(Double.parseDouble(element.getAttribute("azimuth")));
        setting.setInclination(Double.parseDouble(element.getAttribute("inclination")));
        setting.setLog10Distance(Double.parseDouble(element.getAttribute("log10Distance")));
        setting.setIntensity(Double.parseDouble(element.getAttribute("intensity")));

        if (element.hasAttribute("spotSize"))
        {
            setting.setSpotSize(Double.parseDouble(element.getAttribute("spotSize")));
        }

        if (element.hasAttribute("spotTaper"))
        {
            setting.setSpotTaper(Double.parseDouble(element.getAttribute("spotTaper")));
        }

        setting.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));
        setting.setColor(Color.valueOf(element.getAttribute("color")));

        return setting;
    }

}
