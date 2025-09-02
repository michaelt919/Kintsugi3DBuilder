/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.scene.camera;

import javafx.beans.property.*;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.CameraSetting;

public class ObservableCameraSetting extends CameraSetting
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

    public ObservableCameraSetting()
    {
    }

    public ObservableCameraSetting(String name)
    {
        this.setName(name);
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

    public ObservableCameraSetting duplicate()
    {
        ObservableCameraSetting newCamera = new ObservableCameraSetting();
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

    @Override
    public double getXCenter()
    {
        return xCenter.get();
    }

    public DoubleProperty xCenterProperty()
    {
        return xCenter;
    }

    @Override
    public void setXCenter(double xCenter)
    {
        this.xCenter.set(xCenter);
    }

    @Override
    public double getYCenter()
    {
        return yCenter.get();
    }

    public DoubleProperty yCenterProperty()
    {
        return yCenter;
    }

    @Override
    public void setYCenter(double yCenter)
    {
        this.yCenter.set(yCenter);
    }

    @Override
    public double getZCenter()
    {
        return zCenter.get();
    }

    public DoubleProperty zCenterProperty()
    {
        return zCenter;
    }

    @Override
    public void setZCenter(double zCenter)
    {
        this.zCenter.set(zCenter);
    }

    @Override
    public double getAzimuth()
    {
        return azimuth.get();
    }

    public DoubleProperty azimuthProperty()
    {
        return azimuth;
    }

    @Override
    public void setAzimuth(double azimuth)
    {
        this.azimuth.set(azimuth);
    }

    @Override
    public double getInclination()
    {
        return inclination.get();
    }

    public DoubleProperty inclinationProperty()
    {
        return inclination;
    }

    @Override
    public void setInclination(double inclination)
    {
        this.inclination.set(inclination);
    }

    @Override
    public double getLog10Distance()
    {
        return log10Distance.get();
    }

    public DoubleProperty log10DistanceProperty()
    {
        return log10Distance;
    }

    @Override
    public void setLog10Distance(double log10distance)
    {
        this.log10Distance.set(log10distance);
    }

    @Override
    public double getTwist()
    {
        return twist.get();
    }

    public DoubleProperty twistProperty()
    {
        return twist;
    }

    @Override
    public void setTwist(double twist)
    {
        this.twist.set(twist);
    }

    @Override
    public double getFOV()
    {
        return fov.get();
    }

    public DoubleProperty fovProperty()
    {
        return fov;
    }

    @Override
    public void setFOV(double fOV)
    {
        this.fov.set(fOV);
    }

    @Override
    public double getFocalLength()
    {
        return focalLength.get();
    }

    public DoubleProperty focalLengthProperty()
    {
        return focalLength;
    }

    @Override
    public void setFocalLength(double focalLength)
    {
        this.focalLength.set(focalLength);
    }

    @Override
    public boolean isLocked()
    {
        return locked.get();
    }

    public BooleanProperty lockedProperty()
    {
        return locked;
    }

    @Override
    public void setLocked(boolean locked)
    {
        this.locked.set(locked);
    }

    @Override
    public boolean isOrthographic()
    {
        return orthographic.get();
    }

    public BooleanProperty orthographicProperty()
    {
        return orthographic;
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        this.orthographic.set(orthographic);
    }

    @Override
    public String getName()
    {
        return name.get();
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name.set(name);
    }
}
