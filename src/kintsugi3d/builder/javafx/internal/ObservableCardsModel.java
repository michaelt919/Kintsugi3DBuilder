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

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.state.CarouselModel;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class ObservableCardsModel implements CardsModel
{
    private final String label;
    private final String path;

    private final UUIDSelectionModel selectedCardsModel;
    private final UUIDSelectionModel expandedCardsModel;
    private final ObservableList<ProjectDataCard> cardsList;
    private final ObservableList<ProjectDataCard> unmodifiableCardsList; // needs to be here to not get garbage-collected

    private final ProjectDataCardFactory cardFactory;
    private final ObservableCarouselModel carouselModel;

    public ObservableCardsModel(String label, String path, ProjectDataCardFactory cardFactory,
                                ObservableCarouselModel carouselModel)
    {
        this.label = label;
        this.path = path;
        this.cardFactory = cardFactory;
        this.carouselModel = carouselModel;

        cardsList = FXCollections.observableList(new ArrayList<>(8));
        unmodifiableCardsList = FXCollections.unmodifiableObservableList(cardsList);
        selectedCardsModel = new UUIDSelectionModel();
        expandedCardsModel = new UUIDSelectionModel();

        cardsList.addListener((ListChangeListener<? super ProjectDataCard>) change ->
        {
            // Clear selection / expansion for a card when it is removed.
            if (change.wasRemoved())
            {
                for (var removedItem : change.getRemoved())
                {
                    selectedCardsModel.clearSelection(removedItem.getCardId());
                    expandedCardsModel.clearSelection(removedItem.getCardId());
                }
            }
        });
    }

    public void initialize()
    {
        List<ProjectDataCard> dataCards = cardFactory.createAllCards(this);
        this.setCardList(dataCards);
    }

    public String getModelLabel()
    {
        return label;
    }

    public String getPath() {return path; }

    public boolean isSelected(UUID cardId)
    {
        return selectedCardsModel.isSelected(cardId);
    }

    public boolean isExpanded(UUID cardId)
    {
        return expandedCardsModel.isSelected(cardId);
    }

    public BooleanBinding isSelectedProperty(UUID cardId)
    {
        return selectedCardsModel.isSelectedProperty(cardId);
    }

    public BooleanBinding isExpandedProperty(UUID cardId)
    {
        return expandedCardsModel.isSelectedProperty(cardId);
    }

    public void expandCard(UUID id)
    {
        Platform.runLater(() -> expandedCardsModel.select(id));
    }

    public void collapseCard(UUID cardId)
    {
        Platform.runLater(() -> expandedCardsModel.clearSelection(cardId));
    }

    public void selectCard(UUID cardId)
    {
        Platform.runLater(()->selectedCardsModel.select(cardId));
    }

    public void deselectCard(UUID cardId)
    {
        Platform.runLater(()->selectedCardsModel.clearSelection(cardId));
    }

    @Override
    public ObservableList<ProjectDataCard> getCardList()
    {
        return unmodifiableCardsList;
    }

    @Override
    public void setCardList(List<ProjectDataCard> cards)
    {
        cardsList.clear();
        cardsList.addAll(cards);
        selectedCardsModel.clearSelection();
        expandedCardsModel.clearSelection();
    }

    public int getEnabledCardCount()
    {
        int totalEnabled = 0;
        for (ProjectDataCard card : cardsList)
        {
            if (!card.isDisabled())
            {
                ++totalEnabled;
            }
        }
        return totalEnabled;
    }

    public int getDisabledCardCount()
    {
        int totalEnabled = 0;
        for (ProjectDataCard card : cardsList)
        {
            if (card.isDisabled())
            {
                ++totalEnabled;
            }
        }
        return totalEnabled;
    }

    @Override
    public void refreshCards(Predicate<ProjectDataCard> filter)
    {
        // Get a mapping from stale cards needing refresh and their updated version.
        var replacements = cardFactory.createRefreshedCards(this, filter);

        // Loop over cards to check if refresh is needed.
        for (int i = 0; i < cardsList.size(); i++)
        {
            // Look for the old card in the replacement map.
            ProjectDataCard replacement = replacements.get(cardsList.get(i));
            if (replacement != null) // replacement will be null if no refresh is needed.
            {
                // Overwrite the reference to the old card with the new one.
                cardsList.set(i, replacement);
            }
        }
    }

    @Override
    public void deleteCards(Predicate<ProjectDataCard> filter )
    {
        cardsList.removeIf(filter);
    }

    @Override
    public void confirm(String title, String header, String message, Runnable onConfirm)
    {
        // Temp solution -- will eventually create a custom modal.
        Alert alert = new Alert(AlertType.CONFIRMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(header);
        var result = alert.showAndWait();

        if (result.isPresent() && result.get().equals(ButtonType.OK))
        {
            onConfirm.run();
        }
    }

    public CarouselModel getCarousel()
    {
        return carouselModel;
    }
}
