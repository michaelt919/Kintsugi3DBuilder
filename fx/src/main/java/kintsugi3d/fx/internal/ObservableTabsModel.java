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

package kintsugi3d.fx.internal;

import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.cards.ProjectDataCard;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

import java.util.*;
import java.util.stream.Collectors;

public class ObservableTabsModel implements TabsModel
{
    private final ObservableMap<String, ObservableCardsModel<?>> tabs =
        FXCollections.observableMap(new LinkedHashMap<>(4));
    private final ObservableCarouselModel carouselModel;

    private final ObjectProperty<ObservableCardsModel<?>> activeTab = new SimpleObjectProperty<>();
    private final ObjectProperty<ProjectDataCard> lastSelectedCard = new SimpleObjectProperty<>();

    // needs to be here to not get garbage-collected
    private final ObservableMap<String, ObservableCardsModel<?>> unmodifiableTabs =
        FXCollections.unmodifiableObservableMap(tabs);

    public ObservableTabsModel(ObservableCarouselModel carouselModel)
    {
        this.carouselModel = carouselModel;

        for (var tab : tabs.values())
        {
            registerLastSelectedCardListener(tab);
        }

        activeTab.addListener((obs, oldValue, newValue) ->
        {
            // On changing tab, use the last selected card on that tab if no card is selected.
            if (lastSelectedCard.get() == null && newValue != null)
            {
                lastSelectedCard.set(newValue.getLastSelectedCard());
            }
        });
    }

    private void registerLastSelectedCardListener(ObservableCardsModel<?> tab)
    {
        tab.lastSelectedCardProperty().addListener(
            (obs, oldValue, newValue) ->
                // If the old value is still selected, this is presumably a selection event.
                // If the old value is no longer selected, this is presumably a deselection event.
                // Either way, a new card was selected on the current tab, and should be treated as last selected globally.
                lastSelectedCard.set(newValue)
        );
    }

    @Override
    public <T> void addTab(String tabName, ProjectDataCardFactory<T> cardFactory, String path)
    {
        ObservableCardsModel<?> newTab = new ObservableCardsModel<>(tabName, path, cardFactory, carouselModel);
        newTab.initialize();
        tabs.put(tabName, newTab);
        registerLastSelectedCardListener(newTab);
    }

    @Override
    public void clearTabs()
    {
        tabs.clear();

        // Also clear carousel as its contents will be invalidated if the tabs are gone.
        carouselModel.clearCarousel();

        // No selection if the tabs are gone.
        lastSelectedCard.set(null);
    }

    @Override
    public ObservableCardsModel<?> getTab(String label)
    {
        return tabs.get(label);
    }

    @Override
    public <T> ObservableCardsModel<T> getTab(String label, Class<T> dataClass)
    {
        ObservableCardsModel<?> observableCardsModel = tabs.get(label);

        if (Objects.equals(observableCardsModel.getDataClass(), dataClass))
        {
            //noinspection unchecked
            return (ObservableCardsModel<T>) observableCardsModel;
        }
        else
        {
            return null;
        }
    }

    @Override
    public Map<String, ObservableCardsModel<?>> getTabsMap()
    {
        return Collections.unmodifiableMap(tabs);
    }

    public Collection<ObservableCardsModel<?>> getAllTabs()
    {
        return Collections.unmodifiableCollection(tabs.values());
    }

    public ObservableMap<String, ObservableCardsModel<?>> getObservableTabsMap()
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return unmodifiableTabs;
    }

    public ObjectProperty<ObservableCardsModel<?>> activeTabProperty()
    {
        return activeTab;
    }

    public ObservableCardsModel<?> getActiveTab()
    {
        return activeTab.get();
    }

    public void setActiveTab(ObservableCardsModel<?> activeTab)
    {
        this.activeTab.set(activeTab);
    }

    public ObjectExpression<ProjectDataCard> lastSelectedCardProperty()
    {
        return lastSelectedCard;
    }

    public ProjectDataCard getLastSelectedCard()
    {
        return lastSelectedCard.get();
    }

    /**
     * Returns the observable list of selected cards across all tabs
     * @return
     */
    public Collection<ProjectDataCard> getSelectedCards()
    {
        return tabs.values().stream()
            .flatMap(tab -> tab.getSelectedCards().stream())
            .collect(Collectors.toList());
    }
}
