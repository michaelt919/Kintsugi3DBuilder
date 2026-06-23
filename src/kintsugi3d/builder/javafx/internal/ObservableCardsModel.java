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

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObservableCardsModel implements CardsModel
{
    private final String label;
    private final String path;

    private final UUIDSelectionModel selectedCardsModel;
    private final UUIDSelectionModel expandedCardsModel;
    private final ObservableList<ProjectDataCard> cardsList;
    private final ObservableList<ProjectDataCard> unmodifiableCardsList; // needs to be here to not get garbage-collected

    public ObservableCardsModel(String label, String path)
    {
        this.label = label;
        this.path = path;

        cardsList = FXCollections.observableList(new ArrayList<>());
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

    @Override
    public void deleteCard(ProjectDataCard card)
    {
        cardsList.removeIf(other -> other.getCardId().equals(card.getCardId()));
    }

    @Override
    public void confirm(String title, String header, String message, Runnable onConfirm)
    {
        // Temp solution -- will eventually create a custom modal.
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(header);
        var result = alert.showAndWait();

        if (result.isPresent() && result.get().equals(ButtonType.OK))
        {
            onConfirm.run();
        }
    }
}
