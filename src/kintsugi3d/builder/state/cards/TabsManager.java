/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.core.ViewSet;

import java.util.LinkedHashMap;
import java.util.Map;

public class TabsManager
{
    public static final String PHOTOS = "Photos";
    public static final String TEXTURES = "Textures";
    public static final String MATERIALS = "Materials";
    public static final String SHADERS = "Shaders";


    private final Map<String, ProjectDataCardFactory> factories = new LinkedHashMap<>(4);

    public TabsManager(ViewSet viewSet, ProjectInstance<?> instance)
    {
        factories.put(PHOTOS, new CameraCardFactory(viewSet));
        factories.put(TEXTURES, new TextureCardFactory(instance));
        factories.put(MATERIALS, new MaterialCardFactory(instance));
        factories.put(SHADERS, new ShaderCardFactory(instance));
    }

    public TabsManager(ProjectInstance<?> instance)
    {
        this(instance.getActiveViewSet(), instance);
    }

    public void rebuildTabs()
    {
        TabsModel tabsModel = Global.state().getTabModels();
        tabsModel.clearTabs();

        for (var entry : factories.entrySet())
        {
            tabsModel.addTab(entry.getKey(), entry.getValue());
        }
    }

    public void refreshAllTabs()
    {
        TabsModel tabsModel = Global.state().getTabModels();

        for (var tab : tabsModel.getTabsMap().entrySet())
        {
            CardsModel cardsModel = tab.getValue();
            cardsModel.setCardList(factories.get(tab.getKey()).createAllCards(cardsModel));
        }
    }

    public void refreshTab(String tabName)
    {
        TabsModel tabsModel = Global.state().getTabModels();

        CardsModel cardsModel = tabsModel.getTab(tabName);
        cardsModel.setCardList(factories.get(tabName).createAllCards(cardsModel));
    }
}
