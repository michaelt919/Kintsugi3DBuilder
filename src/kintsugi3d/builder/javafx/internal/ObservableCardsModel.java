package kintsugi3d.builder.javafx.internal;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import kintsugi3d.builder.state.CardSelectionModel;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.ProjectDataCard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObservableCardsModel implements CardsModel
{
    private final String label;

    private final CardSelectionModel selectedCardsModel;
    private final CardSelectionModel expandedCardsModel;
    private final ObservableList<ProjectDataCard> cardsList;

    public ObservableCardsModel(String label)
    {
        this.label = label;

        cardsList = FXCollections.observableList(new ArrayList<>());
        selectedCardsModel = new CardSelectionModel(cardsList);
        expandedCardsModel = new CardSelectionModel(cardsList);

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
        return cardsList;
    }

    @Override
    public void setCardList(List<ProjectDataCard> cards)
    {
        Platform.runLater(() ->
        {
            cardsList.clear();
            cardsList.addAll(cards);
            selectedCardsModel.setCardsList(cardsList);
            expandedCardsModel.setCardsList(cardsList);
        });
    }

    @Override
    public int findIndexByCardUUID(UUID id)
    {
        for (int i = 0; i < cardsList.size(); i++)
        {
            UUID uuid = cardsList.get(i).getCardId();
            if (cardsList.get(i).getCardId() == id)
            {
                return i;
            }
        }
        return -1;
    }
}
