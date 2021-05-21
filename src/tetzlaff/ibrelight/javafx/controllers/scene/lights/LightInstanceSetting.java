/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.beans.property.*;
import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tetzlaff.ibrelight.javafx.util.DOMConvertable;
import tetzlaff.ibrelight.javafx.util.StaticUtilities;

public class LightInstanceSetting implements DOMConvertable
{
    private final DoubleProperty targetX = new SimpleDoubleProperty();
    private final DoubleProperty targetY = new SimpleDoubleProperty();
    private final DoubleProperty targetZ = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = StaticUtilities.wrapAround(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty inclination = StaticUtilities.clamp(-90, 90, new SimpleDoubleProperty());
    private final DoubleProperty log10Distance = new SimpleDoubleProperty();
    private final DoubleProperty intensity = StaticUtilities.clamp(0, Double.MAX_VALUE, new SimpleDoubleProperty(1.0));
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty spotSize = StaticUtilities.clamp(0.0, 90.0, new SimpleDoubleProperty(90.0));
    private final DoubleProperty spotTaper = StaticUtilities.clamp(0.0, 1.0, new SimpleDoubleProperty());
    private final Property<Color> color = new SimpleObjectProperty<>(Color.WHITE);

    private final BooleanProperty groupLocked;

    public boolean isGroupLocked()
    {
        return groupLocked.get();
    }

    public LightInstanceSetting(String name, BooleanProperty groupLockedProperty)
    {
        this.name.setValue(name);
        this.groupLocked = groupLockedProperty;
    }

    public LightInstanceSetting duplicate()
    {
        LightInstanceSetting dupl = new LightInstanceSetting(name.getValue(), this.groupLocked);
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
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightInstance");
        element.setAttribute("targetX", targetX.getValue().toString());
        element.setAttribute("targetY", targetY.getValue().toString());
        element.setAttribute("targetZ", targetZ.getValue().toString());
        element.setAttribute("azimuth", azimuth.getValue().toString());
        element.setAttribute("inclination", inclination.getValue().toString());
        element.setAttribute("log10Distance", log10Distance.getValue().toString());
        element.setAttribute("intensity", intensity.getValue().toString());
        element.setAttribute("spotSize", spotSize.getValue().toString());
        element.setAttribute("spotTaper", spotTaper.getValue().toString());
        element.setAttribute("locked", locked.getValue().toString());
        element.setAttribute("name", name.getValue());
        element.setAttribute("color", color.getValue().toString());
        return element;
    }

    public static LightInstanceSetting fromDOMElement(Element element, BooleanProperty groupLockedProperty)
    {
        LightInstanceSetting setting = new LightInstanceSetting(element.getAttribute("name"), groupLockedProperty);
        setting.targetX.set(Double.valueOf(element.getAttribute("targetX")));
        setting.targetY.set(Double.valueOf(element.getAttribute("targetY")));
        setting.targetZ.set(Double.valueOf(element.getAttribute("targetZ")));
        setting.azimuth.set(Double.valueOf(element.getAttribute("azimuth")));
        setting.inclination.set(Double.valueOf(element.getAttribute("inclination")));
        setting.log10Distance.set(Double.valueOf(element.getAttribute("log10Distance")));
        setting.intensity.set(Double.valueOf(element.getAttribute("intensity")));

        if (element.hasAttribute("spotSize"))
        {
            setting.spotSize.set(Double.valueOf(element.getAttribute("spotSize")));
        }

        if (element.hasAttribute("spotTaper"))
        {
            setting.spotTaper.set(Double.valueOf(element.getAttribute("spotTaper")));
        }

        setting.locked.set(Boolean.valueOf(element.getAttribute("locked")));
        setting.color.setValue(Color.valueOf(element.getAttribute("color")));

        return setting;
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
