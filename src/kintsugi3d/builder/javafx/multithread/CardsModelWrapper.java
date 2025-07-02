package kintsugi3d.builder.javafx.multithread;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.List;

public class CardsModelWrapper implements CardsModel {

    private final MultithreadValue<ObservableList<ProjectDataCard>> selectedCards;
    private final MultithreadValue<ObservableList<ProjectDataCard>> expandedCards;
    private final MultithreadValue<ObservableList<ProjectDataCard>> cardList;
    private final CardsModel baseModel;

    public CardsModelWrapper(CardsModel baseModel) {
        this.selectedCards = MultithreadValue.createFromFunctions(baseModel::getSelectedCards, baseModel::setSelectedCards);
        this.expandedCards = MultithreadValue.createFromFunctions(baseModel::getExpandedCards, baseModel::setExpandedCards);
        this.cardList = MultithreadValue.createFromFunctions(baseModel::getObservableCardsList, baseModel::setObservableCardsList);
        this.baseModel = baseModel;
    }

    @Override
    public ObservableList<ProjectDataCard> getSelectedCards() {
        return selectedCards.getValue();
    }

    @Override
    public ObservableList<ProjectDataCard> getExpandedCards() {
        return expandedCards.getValue();
    }

    @Override
    public ObservableSet<String> getSelectedCardIds() {
        return baseModel.getSelectedCardIds();
    }

    @Override
    public ObservableSet<String> getExpandedCardIds() {
        return baseModel.getExpandedCardIds();
    }

    @Override
    public void setSelectedCards(List<ProjectDataCard> cards) {
        baseModel.setSelectedCards(cards);
    }

    @Override
    public void setExpandedCards(List<ProjectDataCard> cards) {
        baseModel.setExpandedCards(cards);
    }

    @Override
    public String getLastSelectedCardId() {
        return baseModel.getLastSelectedCardId();
    }

    @Override
    public String getLastExpandedCardId() {
        return baseModel.getLastExpandedCardId();
    }

    @Override
    public StringProperty getLastSelectedCardProperty() {
        return baseModel.getLastSelectedCardProperty();
    }

    @Override
    public StringProperty getLastExpandedCardProperty() {
        return baseModel.getLastExpandedCardProperty();
    }

    @Override
    public boolean isSelected(String cardId) {
        return baseModel.isSelected(cardId);
    }

    @Override
    public boolean isExpanded(String cardId) {
        return baseModel.isExpanded(cardId);
    }

    @Override
    public BooleanBinding isSelectedProperty(String cardId) {
        return baseModel.isSelectedProperty(cardId);
    }

    @Override
    public BooleanBinding isExpandedProperty(String cardId) {
        return baseModel.isExpandedProperty(cardId);
    }

    @Override
    public void expandCard(String cardId) {
        baseModel.expandCard(cardId);
    }

    @Override
    public void collapseCard(String cardId) {
        baseModel.collapseCard(cardId);
    }

    @Override
    public void selectCard(String cardId) {
        baseModel.selectCard(cardId);
    }

    @Override
    public void deselectCard(String cardId) {
        baseModel.deselectCard(cardId);
    }

    @Override
    public String getModelLabel() {
        return baseModel.getModelLabel();
    }

    @Override
    public Property<ObservableList<ProjectDataCard>> getCardListProperty() {
        return baseModel.getCardListProperty();
    }

    @Override
    public ObservableList<ProjectDataCard> getObservableCardsList() {
        return baseModel.getObservableCardsList();
    }

    @Override
    public void setObservableCardsList(ObservableList<ProjectDataCard> items) {
        baseModel.setObservableCardsList(items);
    }

    @Override
    public void setCardsList(List<ProjectDataCard> cardList) {
        baseModel.setCardsList(cardList);
    }

    @Override
    public void deleteCard(String id) {
        baseModel.deleteCard(id);
    }
}
