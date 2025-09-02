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

package kintsugi3d.builder.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.beans.property.*;
import javafx.scene.paint.Color;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.LightInstanceSetting;

public class ObservableLightInstanceSetting extends LightInstanceSetting
{

    private final DoubleProperty targetX = new SimpleDoubleProperty();
    private final DoubleProperty targetY = new SimpleDoubleProperty();
    private final DoubleProperty targetZ = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = StaticUtilities.wrapAround(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty inclination = StaticUtilities.clamp(-90, 90, new SimpleDoubleProperty());
    private final DoubleProperty log10Distance = new SimpleDoubleProperty();
    private final DoubleProperty intensity = StaticUtilities.clamp(0, Double.MAX_VALUE, new SimpleDoubleProperty(1.0));
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty("X");
    private final DoubleProperty spotSize = StaticUtilities.clamp(0.0, 90.0, new SimpleDoubleProperty(45.0));
    private final DoubleProperty spotTaper = StaticUtilities.clamp(0.0, 1.0, new SimpleDoubleProperty());
    private final Property<Color> color = new SimpleObjectProperty<>(Color.WHITE);

    private final BooleanProperty groupLocked;

    public boolean isGroupLocked()
    {
        return groupLocked.get();
    }

    public ObservableLightInstanceSetting(String name, BooleanProperty groupLockedProperty)
    {
        this.setName(name);
        this.groupLocked = groupLockedProperty;
    }

    public ObservableLightInstanceSetting(BooleanProperty groupLockedProperty)
    {
        this.groupLocked = groupLockedProperty;
    }


    public ObservableLightInstanceSetting duplicate()
    {
        ObservableLightInstanceSetting dupl = new ObservableLightInstanceSetting(name.getValue(), this.groupLocked);
        dupl.targetX.set(this.targetX.get());
        dupl.targetY.set(this.targetY.get());
        dupl.targetZ.set(this.targetZ.get());
        dupl.azimuth.set(this.azimuth.get());
        dupl.inclination.set(this.inclination.get());
        dupl.log10Distance.set(this.log10Distance.get());
        dupl.intensity.set(this.intensity.get());
        dupl.spotSize.set(this.spotSize.get());
        dupl.spotTaper.set(this.spotTaper.get());
        dupl.locked.set(this.locked.get());
        dupl.color.setValue(this.color.getValue());
        return dupl;
    }

    @Override
    public String toString()
    {
        return name.getValue();
    }

    @Override
    public double getTargetX()
    {
        return targetX.get();
    }

    @Override
    public double getTargetY()
    {
        return targetY.get();
    }

    @Override
    public double getTargetZ()
    {
        return targetZ.get();
    }

    @Override
    public double getAzimuth()
    {
        return azimuth.get();
    }

    @Override
    public double getInclination()
    {
        return inclination.get();
    }

    @Override
    public double getLog10Distance()
    {
        return log10Distance.get();
    }

    @Override
    public double getIntensity()
    {
        return intensity.get();
    }

    @Override
    public boolean isLocked()
    {
        return locked.get();
    }

    @Override
    public String getName()
    {
        return name.get();
    }

    @Override
    public double getSpotSize()
    {
        return spotSize.get();
    }

    @Override
    public double getSpotTaper()
    {
        return spotTaper.get();
    }

    @Override
    public Color getColor()
    {
        return color.getValue();
    }

    @Override
    public void setTargetX(double value)
    {
        this.targetX.set(value);
    }

    @Override
    public void setTargetY(double value)
    {
        this.targetY.set(value);
    }

    @Override
    public void setTargetZ(double value)
    {
        this.targetZ.set(value);
    }

    @Override
    public void setAzimuth(double value)
    {
        this.azimuth.set(value);
    }

    @Override
    public void setInclination(double value)
    {
        this.inclination.set(value);
    }

    @Override
    public void setLog10Distance(double value)
    {
        this.log10Distance.set(value);
    }

    @Override
    public void setIntensity(double value)
    {
        this.intensity.set(value);
    }

    @Override
    public void setLocked(boolean value)
    {
        this.locked.set(value);
    }

    @Override
    public void setName(String value)
    {
        this.name.set(value);
    }

    @Override
    public void setSpotSize(double value)
    {
        this.spotSize.set(value);
    }

    @Override
    public void setSpotTaper(double value)
    {
        this.spotTaper.set(value);
    }

    @Override
    public void setColor(Color value)
    {
        this.color.setValue(value);
    }

    public DoubleProperty targetX()
    {
        return targetX;
    }

    public DoubleProperty targetY()
    {
        return targetY;
    }

    public DoubleProperty targetZ()
    {
        return targetZ;
    }

    public DoubleProperty azimuth()
    {
        return azimuth;
    }

    public DoubleProperty inclination()
    {
        return inclination;
    }

    public DoubleProperty log10Distance()
    {
        return log10Distance;
    }

    public DoubleProperty intensity()
    {
        return intensity;
    }

    public BooleanProperty locked()
    {
        return locked;
    }

    public StringProperty name()
    {
        return name;
    }

    public Property<Color> color()
    {
        return color;
    }

    public DoubleProperty spotSize()
    {
        return spotSize;
    }

    public DoubleProperty spotTaper()
    {
        return spotTaper;
    }
}
