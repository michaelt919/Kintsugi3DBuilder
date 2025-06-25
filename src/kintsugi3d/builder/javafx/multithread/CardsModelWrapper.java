package kintsugi3d.builder.javafx.multithread;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;
import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.List;

public class CardsModelWrapper implements CardsModel {

    private final MultithreadValue<Integer> selectedCardIndex;
    private final MultithreadValue<List<ProjectDataCard>> cardList;
    private final CardsModel baseModel;

    public CardsModelWrapper(CardsModel baseModel) {
        this.selectedCardIndex = MultithreadValue.createFromFunctions(baseModel::getSelectedCardIndex, baseModel::setSelectedCardIndex);
        this.cardList = MultithreadValue.createFromFunctions(baseModel::getCardsList, baseModel::setCardsList);
        this.baseModel = baseModel;
    }

    @Override
    public ProjectDataCard getSelectedCard() {
        return baseModel.getCardsList().get(baseModel.getSelectedCardIndex());
    }

    @Override
    public int getSelectedCardIndex() {
        return baseModel.getSelectedCardIndex();
    }

    @Override
    public void setSelectedCardIndex(int cardIndex) {
        baseModel.setSelectedCardIndex(cardIndex);
    }

    @Override
    public ReadOnlyIntegerProperty getSelectedCardIndexProperty() {
        return baseModel.getSelectedCardIndexProperty();
    }

    @Override
    public List<ProjectDataCard> getCardsList() {
        return baseModel.getCardsList();
    }

    @Override
    public ObservableList<ProjectDataCard> getItems() {
        return baseModel.getItems();
    }

    @Override
    public String getLabel() {
        return baseModel.getLabel();
    }

    @Override
    public void setCardsList(List<ProjectDataCard> cardsList) {
        baseModel.setCardsList(cardsList);
    }

    @Override
    public void deselectCard() {
        baseModel.deselectCard();
    }

    @Override
    public void replaceCard(int index) {

    }

    @Override
    public void deleteCard(int index) {
        baseModel.deleteCard(index);
    }
}
