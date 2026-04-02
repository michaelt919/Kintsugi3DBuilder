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

public abstract class SerializableCameraSettings implements DOMConvertable, CameraSettings
{
    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Camera");
        element.setAttribute("lookAtX", Double.toString(getXCenter()));
        element.setAttribute("lookAtY", Double.toString(getYCenter()));
        element.setAttribute("lookAtZ", Double.toString(getZCenter()));
        element.setAttribute("azimuth", Double.toString(getAzimuth()));
        element.setAttribute("inclination", Double.toString(getInclination()));
        element.setAttribute("log10Distance", Double.toString(getLog10Distance()));
        element.setAttribute("twist", Double.toString(getTwist()));
        element.setAttribute("fov", Double.toString(getFOV()));
        element.setAttribute("locked", Boolean.toString(isLocked()));
        element.setAttribute("orthographic", Boolean.toString(isOrthographic()));
        element.setAttribute("name", getName());
        return element;
    }

    public static <CameraSettingType extends SerializableCameraSettings> CameraSettingType
    fromDOMElement(Element element, Supplier<CameraSettingType> cameraSettingConstructor)
    {
        CameraSettingType newCamera = cameraSettingConstructor.get();
        newCamera.setXCenter(Double.parseDouble(element.getAttribute("lookAtX")));
        newCamera.setYCenter(Double.parseDouble(element.getAttribute("lookAtY")));
        newCamera.setZCenter(Double.parseDouble(element.getAttribute("lookAtZ")));
        newCamera.setAzimuth(Double.parseDouble(element.getAttribute("azimuth")));
        newCamera.setInclination(Double.parseDouble(element.getAttribute("inclination")));
        newCamera.setLog10Distance(Double.parseDouble(element.getAttribute("log10Distance")));
        newCamera.setTwist(Double.parseDouble(element.getAttribute("twist")));
        newCamera.setFOV(Double.parseDouble(element.getAttribute("fov")));
        newCamera.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));
        newCamera.setOrthographic(Boolean.parseBoolean(element.getAttribute("orthographic")));
        newCamera.setName(element.getAttribute("name"));
        return newCamera;
    }

}
