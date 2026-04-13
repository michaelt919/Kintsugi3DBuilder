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

package kintsugi3d.builder.javafx.internal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.cards.ProjectDataCard;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

import java.util.*;

public class ObservableTabsModel implements TabsModel
{
    private final ObservableMap<String, ObservableCardsModel> tabs;

    public ObservableTabsModel()
    {
        Map<String, ObservableCardsModel> cardsModels = new LinkedHashMap<>(4);
        tabs = FXCollections.observableMap(cardsModels);
    }

    @Override
    public void addTab(String tabName, ProjectDataCardFactory cardFactory)
    {
        ObservableCardsModel newTab = new ObservableCardsModel(tabName);
        List<ProjectDataCard> dataCards = cardFactory.createAllCards(newTab);
        newTab.setCardList(dataCards);
        tabs.put(tabName, newTab);
    }

    @Override
    public void clearTabs()
    {
        tabs.clear();
    }

    @Override
    public ObservableCardsModel getTab(String label)
    {
        return tabs.get(label);
    }

    @Override
    public Map<String, ObservableCardsModel> getTabsMap()
    {
        return Collections.unmodifiableMap(tabs);
    }

    public Collection<ObservableCardsModel> getAllTabs()
    {
        return Collections.unmodifiableCollection(tabs.values());
    }

    public ObservableMap<String, ObservableCardsModel> getObservableTabsMap()
    {
        return tabs;
    }
}
