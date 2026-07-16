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

package kintsugi3d.builder.javafx.internal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

import java.util.*;

public class ObservableTabsModel implements TabsModel
{
    private final ObservableMap<String, ObservableCardsModel> tabs;
    private final ObservableCarouselModel carouselModel;
    ObservableList<String> selectedCards = FXCollections.observableArrayList();
    List<String> selectedCardNames = new ArrayList<>();


    public ObservableTabsModel(ObservableCarouselModel carouselModel)
    {
        this.carouselModel = carouselModel;

        Map<String, ObservableCardsModel> cardsModels = new LinkedHashMap<>(4);
        this.tabs = FXCollections.observableMap(cardsModels);
    }

    @Override
    public void addTab(String tabName, ProjectDataCardFactory cardFactory, String path)
    {
        ObservableCardsModel newTab = new ObservableCardsModel(tabName, path, cardFactory, carouselModel);
        newTab.initialize();
        tabs.put(tabName, newTab);
    }

    @Override
    public void clearTabs()
    {
        tabs.clear();

        // Also clear carousel as its contents will be invalidated if the tabs are gone.
        carouselModel.clearCarousel();
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

    /**
     * Adds the given fileName to selectedCards if it is not already there.
     * If it is there it will remove it.
     * @param fileName
     */
    @Override
    public void addSelected(String filePath, String fileName)
    {
        boolean found = false; //Initializes to false
        if (selectedCards != null)
        {
            for (String current : selectedCards) //Every fileName in selectedCards
            {
                if (current.equals(filePath)) //If a fileName in selectedCards matches given fileName
                {
                    selectedCards.remove(filePath); //Remove fileName from selected cards
                    selectedCardNames.remove(fileName);
                    found = true; //Make found true
                }
            }
            if (!found) //If fileName was not found
            {
                selectedCards.add(filePath); //Add fileName to selected Cards
                selectedCardNames.add(fileName);
            }
        }
    }

    /**
     * Clears all fileNames from selected
     */
    @Override
    public void clearSelected()
    {
        selectedCards.clear();
        selectedCardNames.clear();
    }

    /**
     * Returns the observable list of fileNames
     * @return
     */
    public ObservableList<String> getAllCards()
    {
        return selectedCards;
    }

    public String getFileName(String filePath)
    {
        return selectedCardNames.get(selectedCards.indexOf(filePath));
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
