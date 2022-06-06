/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.scene.camera;

import javafx.beans.property.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tetzlaff.ibrelight.javafx.util.DOMConvertable;
import tetzlaff.ibrelight.javafx.util.StaticUtilities;

public class CameraSetting implements DOMConvertable
{
    private final DoubleProperty xCenter = new SimpleDoubleProperty();
    private final DoubleProperty yCenter = new SimpleDoubleProperty();
    private final DoubleProperty zCenter = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = StaticUtilities.wrapAround(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty inclination = StaticUtilities.clamp(-90, 90, new SimpleDoubleProperty());
    private final DoubleProperty log10Distance = new SimpleDoubleProperty(Math.log10(25.0 / 18.0));
    private final DoubleProperty twist = StaticUtilities.wrapAround(-180.0, 180.0, new SimpleDoubleProperty());
    private final DoubleProperty fov = new SimpleDoubleProperty(
        360 / Math.PI /* convert and multiply by 2) */ * Math.atan(0.36 /* "35 mm" film (actual 36mm horizontal), 50mm lens */));
    private final DoubleProperty focalLength = new SimpleDoubleProperty(50.0);
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final BooleanProperty orthographic = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty("New Camera");

    // TODO FOV and focal length shouldn't both be set.  In fact, this constructor could probably be replaced with the default and setters used to initialize it instead.

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Camera");
        element.setAttribute("lookAtX", xCenter.getValue().toString());
        element.setAttribute("lookAtY", yCenter.getValue().toString());
        element.setAttribute("lookAtZ", zCenter.getValue().toString());
        element.setAttribute("azimuth", azimuth.getValue().toString());
        element.setAttribute("inclination", inclination.getValue().toString());
        element.setAttribute("log10Distance", log10Distance.getValue().toString());
        element.setAttribute("twist", twist.getValue().toString());
        element.setAttribute("fov", fov.getValue().toString());
        element.setAttribute("locked", locked.getValue().toString());
        element.setAttribute("orthographic", orthographic.getValue().toString());
        element.setAttribute("name", name.getValue());
        return element;
    }

    public static CameraSetting fromDOMElement(Element element)
    {
        CameraSetting newCamera = new CameraSetting();
        newCamera.setXCenter(Double.valueOf(element.getAttribute("lookAtX")));
        newCamera.setYCenter(Double.valueOf(element.getAttribute("lookAtY")));
        newCamera.setZCenter(Double.valueOf(element.getAttribute("lookAtZ")));
        newCamera.setAzimuth(Double.valueOf(element.getAttribute("azimuth")));
        newCamera.setInclination(Double.valueOf(element.getAttribute("inclination")));
        newCamera.setLog10Distance(Double.valueOf(element.getAttribute("log10Distance")));
        newCamera.setTwist(Double.valueOf(element.getAttribute("twist")));
        newCamera.setFOV(Double.valueOf(element.getAttribute("fov")));
        newCamera.setLocked(Boolean.valueOf(element.getAttribute("locked")));
        newCamera.setOrthographic(Boolean.valueOf(element.getAttribute("orthographic")));
        newCamera.setName(element.getAttribute("name"));
        return newCamera;
    }

    @Override
    public String toString()
    {
        if (locked.getValue())
        {
            return "(L) " + name.getValue();
        }
        else
        {
            return name.getValue();
        }
    }

    public CameraSetting duplicate()
    {
        CameraSetting newCamera = new CameraSetting();
        newCamera.setXCenter(this.xCenter.getValue());
        newCamera.setYCenter(this.yCenter.getValue());
        newCamera.setZCenter(this.zCenter.getValue());
        newCamera.setAzimuth(this.azimuth.getValue());
        newCamera.setInclination(this.inclination.getValue());
        newCamera.setLog10Distance(this.log10Distance.getValue());
        newCamera.setTwist(this.twist.getValue());
        newCamera.setFOV(this.fov.getValue());
        newCamera.setLocked(this.locked.getValue());
        newCamera.setOrthographic(this.orthographic.getValue());
        newCamera.setName(this.name.getValue() + " copy");
        return newCamera;
    }

    public double getXCenter()
    {
        return xCenter.get();
    }

    public DoubleProperty xCenterProperty()
    {
        return xCenter;
    }

    public void setXCenter(double xCenter)
    {
        this.xCenter.set(xCenter);
    }

    public double getYCenter()
    {
        return yCenter.get();
    }

    public DoubleProperty yCenterProperty()
    {
        return yCenter;
    }

    public void setYCenter(double yCenter)
    {
        this.yCenter.set(yCenter);
    }

    public double getZCenter()
    {
        return zCenter.get();
    }

    public DoubleProperty zCenterProperty()
    {
        return zCenter;
    }

    public void setZCenter(double zCenter)
    {
        this.zCenter.set(zCenter);
    }

    public double getAzimuth()
    {
        return azimuth.get();
    }

    public DoubleProperty azimuthProperty()
    {
        return azimuth;
    }

    public void setAzimuth(double azimuth)
    {
        this.azimuth.set(azimuth);
    }

    public double getInclination()
    {
        return inclination.get();
    }

    public DoubleProperty inclinationProperty()
    {
        return inclination;
    }

    public void setInclination(double inclination)
    {
        this.inclination.set(inclination);
    }

    public double getLog10Distance()
    {
        return log10Distance.get();
    }

    public DoubleProperty log10DistanceProperty()
    {
        return log10Distance;
    }

    public void setLog10Distance(double log10distance)
    {
        this.log10Distance.set(log10distance);
    }

    public double getTwist()
    {
        return twist.get();
    }

    public DoubleProperty twistProperty()
    {
        return twist;
    }

    public void setTwist(double twist)
    {
        this.twist.set(twist);
    }

    public double getFOV()
    {
        return fov.get();
    }

    public DoubleProperty fovProperty()
    {
        return fov;
    }

    public void setFOV(double fOV)
    {
        this.fov.set(fOV);
    }

    public double getFocalLength()
    {
        return focalLength.get();
    }

    public DoubleProperty focalLengthProperty()
    {
        return focalLength;
    }

    public void setFocalLength(double focalLength)
    {
        this.focalLength.set(focalLength);
    }

    public boolean isLocked()
    {
        return locked.get();
    }

    public BooleanProperty lockedProperty()
    {
        return locked;
    }

    public void setLocked(boolean locked)
    {
        this.locked.set(locked);
    }

    public boolean isOrthographic()
    {
        return orthographic.get();
    }

    public BooleanProperty orthographicProperty()
    {
        return orthographic;
    }

    public void setOrthographic(boolean orthographic)
    {
        this.orthographic.set(orthographic);
    }

    public String getName()
    {
        return name.get();
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name.set(name);
    }
}
