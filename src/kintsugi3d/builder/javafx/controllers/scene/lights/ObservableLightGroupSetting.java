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

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import kintsugi3d.builder.state.project.LightGroupSetting;

import java.util.ArrayList;

public class ObservableLightGroupSetting extends LightGroupSetting<ObservableLightInstanceSetting>
{
    public static final int LIGHT_LIMIT = 4;

    private final ListProperty<ObservableLightInstanceSetting> lightList = new SimpleListProperty<>(new ObservableListWrapper<>(new ArrayList<>(LIGHT_LIMIT)));
    private final BooleanProperty locked = new SimpleBooleanProperty(false);
    private final StringProperty name = new SimpleStringProperty("New Group");

    private final IntegerProperty selectedLightIndex = new SimpleIntegerProperty(0);

    public ObservableLightGroupSetting()
    {
    }

    public ObservableLightGroupSetting(String name)
    {
        this.setName(name);
    }

    @Override
    protected ObservableLightInstanceSetting constructLightInstanceSetting()
    {
        return new ObservableLightInstanceSetting(locked);
    }

    public void addLight()
    {
        if (lightList.size() < LIGHT_LIMIT)
        {
            lightList.add(new ObservableLightInstanceSetting(locked));
        }
    }

    @Override
    public void addLight(int index, double targetX, double targetY, double targetZ)
    {
        if (lightList.size() < LIGHT_LIMIT)
        {
            if (index >= 0 && index < lightList.size())
            {
                ObservableLightInstanceSetting newLight = lightList.get(index).duplicate();
                newLight.targetX().set(targetX);
                newLight.targetY().set(targetY);
                newLight.targetZ().set(targetZ);
                lightList.add(newLight);
            }
            else
            {
                addLight();
            }
        }
    }

    @Override
    public void removeLight(int index)
    {
        if (!lightList.isEmpty())
        {
            if (index >= 0 && index < lightList.size())
            {
                lightList.remove(index);
            }
        }
    }

    @Override
    public int getLightCount()
    {
        return lightList.size();
    }

    @Override
    public ObservableList<ObservableLightInstanceSetting> getLightList()
    {
        return lightList.get();
    }

    public ListProperty<ObservableLightInstanceSetting> lightListProperty()
    {
        return lightList;
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

    public IntegerProperty selectedLightIndexProperty()
    {
        return selectedLightIndex;
    }

    public int getSelectedLightIndex()
    {
        return selectedLightIndex.getValue();
    }

    public void setSelectedLightIndex(int selectedLightIndex)
    {
        this.selectedLightIndex.setValue(selectedLightIndex);
    }
}
