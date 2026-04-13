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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.function.Supplier;

public abstract class SerializableLightGroupSettings<LightInstanceSettingType extends SerializableLightSettings> implements DOMConvertable, LightGroupSettings<LightInstanceSettingType>
{

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightGroup");
        element.setAttribute("name", getName());
        element.setAttribute("locked", Boolean.toString(isLocked()));

        for (SerializableLightSettings lightInstance : getLightList())
        {
            element.appendChild(lightInstance.toDOMElement(document));
        }

        return element;
    }

    public static <LightGroupSettingType extends SerializableLightGroupSettings<LightInstanceSettingType>, LightInstanceSettingType extends SerializableLightSettings>
    LightGroupSettingType fromDOMElement(Element element, Supplier<LightGroupSettingType> lightGroupSettingConstructor)
    {
        LightGroupSettingType lightGroup = lightGroupSettingConstructor.get();
        lightGroup.setName(element.getAttribute("name"));
        lightGroup.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (child instanceof Element)
            {
                lightGroup.getLightList().add(SerializableLightSettings.fromDOMElement((Element)child, lightGroup::constructLightInstanceSetting));
            }
        }

        return lightGroup;
    }

}
