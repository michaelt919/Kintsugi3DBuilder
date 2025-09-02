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

package kintsugi3d.builder.javafx.controllers.scene.object;

import javafx.beans.property.*;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.project.ObjectPoseSetting;

public class ObservableObjectPoseSetting extends ObjectPoseSetting
{
    private final DoubleProperty centerX = new SimpleDoubleProperty();
    private final DoubleProperty centerY = new SimpleDoubleProperty();
    private final DoubleProperty centerZ = new SimpleDoubleProperty();
    private final DoubleProperty rotateY = StaticUtilities.wrapAround(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty rotateX = StaticUtilities.clamp(-90, 90, new SimpleDoubleProperty());
    private final DoubleProperty rotateZ = StaticUtilities.wrapAround(-180.0, 180.0, new SimpleDoubleProperty());
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty("Default Pose");

    private final DoubleProperty scale = new SimpleDoubleProperty(1.0);

    public ObservableObjectPoseSetting()
    {
    }

    public ObservableObjectPoseSetting(String name)
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

    public ObservableObjectPoseSetting duplicate()
    {
        ObservableObjectPoseSetting copy = new ObservableObjectPoseSetting();

        copy.setCenterX(this.getCenterX());
        copy.setCenterY(this.getCenterY());
        copy.setCenterZ(this.getCenterZ());

        copy.setRotateY(this.getRotateY());
        copy.setRotateX(this.getRotateX());
        copy.setRotateZ(this.getRotateZ());

        copy.setLocked(this.isLocked());
        copy.setScale(this.getScale());
        copy.setName(this.getName() + " copy");

        return copy;
    }

    @Override
    public double getCenterX()
    {
        return centerX.get();
    }

    public DoubleProperty centerXProperty()
    {
        return centerX;
    }

    @Override
    public void setCenterX(double centerX)
    {
        this.centerX.set(centerX);
    }

    @Override
    public double getCenterY()
    {
        return centerY.get();
    }

    public DoubleProperty centerYProperty()
    {
        return centerY;
    }

    @Override
    public void setCenterY(double centerY)
    {
        this.centerY.set(centerY);
    }

    @Override
    public double getCenterZ()
    {
        return centerZ.get();
    }

    public DoubleProperty centerZProperty()
    {
        return centerZ;
    }

    @Override
    public void setCenterZ(double centerZ)
    {
        this.centerZ.set(centerZ);
    }

    @Override
    public double getRotateY()
    {
        return rotateY.get();
    }

    public DoubleProperty rotateYProperty()
    {
        return rotateY;
    }

    @Override
    public void setRotateY(double rotateY)
    {
        this.rotateY.set(rotateY);
    }

    @Override
    public double getRotateX()
    {
        return rotateX.get();
    }

    public DoubleProperty rotateXProperty()
    {
        return rotateX;
    }

    @Override
    public void setRotateX(double rotateX)
    {
        this.rotateX.set(rotateX);
    }

    @Override
    public double getRotateZ()
    {
        return rotateZ.get();
    }

    public DoubleProperty rotateZProperty()
    {
        return rotateZ;
    }

    @Override
    public void setRotateZ(double rotateZ)
    {
        this.rotateZ.set(rotateZ);
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

    public DoubleProperty scaleProperty(){return scale;}

    @Override
    public double getScale(){return Math.pow(10, scale.get());}
        //scale.get() returns the slider value before it has been adjusted to a logarithmic scale

    @Override
    public void setScale(double scale){this.scale.set(scale);}

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
