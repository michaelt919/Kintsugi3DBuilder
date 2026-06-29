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

package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class SynchronizedTabsModel implements TabsModel
{
    private final TabsModel base;

    public SynchronizedTabsModel(TabsModel base)
    {
        this.base = base;
    }

    @Override
    public void addTab(String tabName, ProjectDataCardFactory cardFactory, String path)
    {
        Platform.runLater(() -> base.addTab(tabName, cardFactory, path));
    }

    @Override
    public void clearTabs()
    {
        Platform.runLater(base::clearTabs);
    }

    @Override
    public CardsModel getTab(String label)
    {
        return new SynchronizedCardsModel(base.getTab(label));
    }

    @Override
    public Map<String, CardsModel> getTabsMap()
    {
        Map<String, CardsModel> wrapped = new LinkedHashMap<>();

        for (var entry : base.getTabsMap().entrySet())
        {
            wrapped.put(entry.getKey(), new SynchronizedCardsModel(entry.getValue()));
        }

        return wrapped;
    }
}
