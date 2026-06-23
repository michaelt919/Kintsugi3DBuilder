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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TabsManager
{
    public static final String PHOTOS = "Photos";
    public static final String TEXTURES = "Textures";
    public static final String MATERIALS = "Materials";
    public static final String SHADERS = "Shaders";

    private final List<TabInfo> factories = new ArrayList<>();

    public TabsManager(ViewSet viewSet, ProjectInstance<?> instance)
    {
        factories.add(new TabInfo(PHOTOS, new CameraCardFactory(viewSet), null));
        factories.add(new TabInfo(TEXTURES, new TextureCardFactory(instance), Global.state().getIOModel().getLoadedViewSet().getSupportingFilesDirectory().getPath()));
        factories.add(new TabInfo(MATERIALS, new MaterialCardFactory(instance), Global.state().getIOModel().getLoadedViewSet().getSupportingFilesDirectory().getPath()));
        factories.add(new TabInfo(SHADERS, new ShaderCardFactory(instance), null));
    }

    public TabsManager(ProjectInstance<?> instance)
    {
        this(instance.getActiveViewSet(), instance);
    }

    public void rebuildTabs()
    {
        TabsModel tabsModel = Global.state().getTabModels();
        tabsModel.clearTabs();

        for (var entry : factories)
        {
            tabsModel.addTab(entry.getLabel(), entry.getFactory(), entry.getPath());
        }
    }

    public void refreshAllTabs()
    {
        TabsModel tabsModel = Global.state().getTabModels();
        int index = 0;
        for (var tab : tabsModel.getTabsMap().entrySet())
        {
            CardsModel cardsModel = tab.getValue();
            cardsModel.setCardList(factories.get(index).getFactory().createAllCards(cardsModel));
            index += 1;
        }
    }

    public void refreshTab(String tabName)
    {
        TabsModel tabsModel = Global.state().getTabModels();

        CardsModel cardsModel = tabsModel.getTab(tabName);
        for (var fac : factories){
            if (fac.getLabel().equals(tabName)){
                cardsModel.setCardList(fac.getFactory().createAllCards(cardsModel));
            }
        }
    }
}
