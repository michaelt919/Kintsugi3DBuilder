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

import java.util.ArrayList;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import kintsugi3d.builder.javafx.util.DOMConvertable;

public class LightGroupSetting implements DOMConvertable
{
    public static final int LIGHT_LIMIT = 4;

    private final ListProperty<LightInstanceSetting> lightList = new SimpleListProperty<>(new ObservableListWrapper<>(new ArrayList<>(LIGHT_LIMIT)));
    private final BooleanProperty locked = new SimpleBooleanProperty(false);
    private final StringProperty name = new SimpleStringProperty();

    private final IntegerProperty selectedLightIndex = new SimpleIntegerProperty(0);

    public LightGroupSetting(String name)
    {
        this.name.setValue(name);
    }

    public void addLight()
    {
        if (lightList.size() < LIGHT_LIMIT)
        {
            lightList.add(new LightInstanceSetting("X", locked));
        }
    }

    public void addLight(int index, double targetX, double targetY, double targetZ)
    {
        if (lightList.size() < LIGHT_LIMIT)
        {
            if (index >= 0 && index < lightList.size())
            {
                LightInstanceSetting newLight = lightList.get(index).duplicate();
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

    public void removeLight()
    {
        if (!lightList.isEmpty())
        {
            lightList.remove(lightList.size() - 1);
        }
    }

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
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightGroup");
        element.setAttribute("name", name.getValue());
        element.setAttribute("locked", locked.getValue().toString());

        for(LightInstanceSetting lightInstance : lightList)
        {
            element.appendChild(lightInstance.toDOMElement(document));
        }

        return element;
    }

    public static LightGroupSetting fromDOMElement(Element element)
    {
        LightGroupSetting lightGroup = new LightGroupSetting(element.getAttribute("name"));
        lightGroup.setLocked(Boolean.valueOf(element.getAttribute("locked")));

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (child instanceof Element)
            {
                lightGroup.lightList.add(LightInstanceSetting.fromDOMElement((Element)child, lightGroup.locked));
            }
        }

        return lightGroup;
    }

    public int getLightCount()
    {
        return lightList.size();
    }

    public ObservableList<LightInstanceSetting> getLightList()
    {
        return lightList.get();
    }

    public ListProperty<LightInstanceSetting> lightListProperty()
    {
        return lightList;
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
