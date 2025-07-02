package kintsugi3d.builder.state;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import kintsugi3d.builder.resources.ProjectDataCard;

import java.util.List;

public interface CardsModel {
    ObservableList<ProjectDataCard> getSelectedCards();
    ObservableList<ProjectDataCard> getExpandedCards();
    ObservableSet<String> getSelectedCardIds();
    ObservableSet<String> getExpandedCardIds();
    void setSelectedCards(List<ProjectDataCard> cards);
    void setExpandedCards(List<ProjectDataCard> cards);

    String getLastSelectedCardId();
    String getLastExpandedCardId();
    StringProperty getLastSelectedCardProperty();
    StringProperty getLastExpandedCardProperty();

    boolean isSelected(String cardId);
    boolean isExpanded(String cardId);
    BooleanBinding isSelectedProperty(String cardId);
    BooleanBinding isExpandedProperty(String cardId);

    void expandCard(String cardId);
    void collapseCard(String cardId);
    void selectCard(String cardId);
    void deselectCard(String cardId);

    String getModelLabel();
    Property<ObservableList<ProjectDataCard>> getCardListProperty();
    ObservableList<ProjectDataCard> getObservableCardsList();
    void setObservableCardsList(ObservableList<ProjectDataCard> items);
    void setCardsList(List<ProjectDataCard> cardList);
    void deleteCard(String id);
}
