package kintsugi3d.builder.state;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import kintsugi3d.builder.resources.ProjectDataCard;

import java.util.List;
import java.util.UUID;

public interface CardsModel {
    ObservableList<ProjectDataCard> getSelectedCards();
    ObservableList<ProjectDataCard> getExpandedCards();
    ObservableSet<UUID> getSelectedCardIds();
    ObservableSet<UUID> getExpandedCardIds();
    void setSelectedCards(List<ProjectDataCard> cards);
    void setExpandedCards(List<ProjectDataCard> cards);

    UUID getLastSelectedCardId();
    UUID getLastExpandedCardId();
    ObjectProperty<UUID> getLastSelectedCardProperty();
    ObjectProperty<UUID> getLastExpandedCardProperty();

    boolean isSelected(UUID cardId);
    boolean isExpanded(UUID cardId);
    BooleanBinding isSelectedProperty(UUID cardId);
    BooleanBinding isExpandedProperty(UUID cardId);

    void expandCard(UUID cardId);
    void collapseCard(UUID cardId);
    void selectCard(UUID cardId);
    void deselectCard(UUID cardId);

    String getModelLabel();
    ObservableList<ProjectDataCard> getObservableCardsList();
    void setObservableCardsList(ObservableList<ProjectDataCard> items);
    void setCardsList(List<ProjectDataCard> cardList);
    void deleteCard(UUID id);
}
