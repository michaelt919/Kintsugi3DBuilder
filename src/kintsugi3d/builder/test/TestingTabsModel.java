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

package kintsugi3d.builder.test;

import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

import java.util.Map;

public class TestingTabsModel implements TabsModel
{
    @Override
    public <T> void addTab(String tabName, ProjectDataCardFactory<T> cardFactory, String path)
    {
    }

    @Override
    public void clearTabs()
    {
    }

    @Override
    public CardsModel<?> getTab(String label)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> CardsModel<T> getTab(String label, Class<T> dataClass)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ? extends CardsModel<?>> getTabsMap()
    {
        return Map.of();
    }
}
