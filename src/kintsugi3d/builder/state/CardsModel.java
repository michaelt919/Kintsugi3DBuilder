package kintsugi3d.builder.state;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;
import kintsugi3d.builder.resources.ProjectDataCard;

import java.util.List;

public interface CardsModel {
    ProjectDataCard getSelectedCard();
    int getSelectedCardIndex();
    void setSelectedCardIndex(int cardViewIndex);
    ReadOnlyIntegerProperty getSelectedCardIndexProperty();
    List<ProjectDataCard> getCardsList();
    ObservableList<ProjectDataCard> getItems();
    String getLabel();
    void setCardsList(List<ProjectDataCard> cameraList);
    void deselectCard();
    void replaceCard(int index);
    void deleteCard(int index);
}
