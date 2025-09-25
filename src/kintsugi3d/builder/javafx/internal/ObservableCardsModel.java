package kintsugi3d.builder.javafx.internal;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObservableCardsModel implements CardsModel
{
    private final String label;

    private final UUIDSelectionModel selectedCardsModel;
    private final UUIDSelectionModel expandedCardsModel;
    private final ObservableList<ProjectDataCard> cardsList;
    private final ObservableList<ProjectDataCard> unmodifiableCardsList; // needs to be here to not get garbage-collected

    public ObservableCardsModel(String label)
    {
        this.label = label;

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
}
