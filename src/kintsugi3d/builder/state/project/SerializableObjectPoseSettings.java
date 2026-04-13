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

import java.util.function.Supplier;

public abstract class SerializableObjectPoseSettings implements DOMConvertable, ObjectPoseSettings
{
    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Camera");
        element.setAttribute("centerX", Double.toString(getCenterX()));
        element.setAttribute("centerY", Double.toString(getCenterY()));
        element.setAttribute("centerZ", Double.toString(getCenterZ()));
        element.setAttribute("rotateY", Double.toString(getRotateY()));
        element.setAttribute("rotateX", Double.toString(getRotateX()));
        element.setAttribute("rotateZ", Double.toString(getRotateZ()));
        element.setAttribute("locked", Boolean.toString(isLocked()));
        element.setAttribute("scale", Double.toString(getScale()));
        element.setAttribute("name", getName());
        return element;
    }

    public static <ObjectPoseSettingType extends SerializableObjectPoseSettings>
    ObjectPoseSettingType fromDOMElement(Element element, Supplier<ObjectPoseSettingType> objectPoseSettingConstructor)
    {
        ObjectPoseSettingType setting = objectPoseSettingConstructor.get();
        setting.setName(element.getAttribute("name"));
        setting.setCenterX(Double.parseDouble(element.getAttribute("centerX")));
        setting.setCenterY(Double.parseDouble(element.getAttribute("centerY")));
        setting.setCenterZ(Double.parseDouble(element.getAttribute("centerZ")));
        setting.setRotateY(Double.parseDouble(element.getAttribute("rotateY")));
        setting.setRotateX(Double.parseDouble(element.getAttribute("rotateX")));
        setting.setRotateZ(Double.parseDouble(element.getAttribute("rotateZ")));
        setting.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));

        setting.setScale(element.hasAttribute("scale")
            ? Double.parseDouble(element.getAttribute("scale"))
            : 1.0);

        return setting;
    }

}
