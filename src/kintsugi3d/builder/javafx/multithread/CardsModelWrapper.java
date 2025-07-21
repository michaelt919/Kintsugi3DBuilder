package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.List;
import java.util.UUID;

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
    public ObservableSet<UUID> getSelectedCardIds() {
        return baseModel.getSelectedCardIds();
    }

    @Override
    public ObservableSet<UUID> getExpandedCardIds() {
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
    public UUID getLastSelectedCardId() {
        return baseModel.getLastSelectedCardId();
    }

    @Override
    public UUID getLastExpandedCardId() {
        return baseModel.getLastExpandedCardId();
    }

    @Override
    public ObjectProperty<UUID> getLastSelectedCardProperty() {
        return baseModel.getLastSelectedCardProperty();
    }

    @Override
    public ObjectProperty<UUID> getLastExpandedCardProperty() {
        return baseModel.getLastExpandedCardProperty();
    }

    @Override
    public boolean isSelected(UUID cardId) {
        return baseModel.isSelected(cardId);
    }

    @Override
    public boolean isExpanded(UUID cardId) {
        return baseModel.isExpanded(cardId);
    }

    @Override
    public BooleanBinding isSelectedProperty(UUID cardId) {
        return baseModel.isSelectedProperty(cardId);
    }

    @Override
    public BooleanBinding isExpandedProperty(UUID cardId) {
        return baseModel.isExpandedProperty(cardId);
    }

    @Override
    public void expandCard(UUID cardId) {
        baseModel.expandCard(cardId);
    }

    @Override
    public void collapseCard(UUID cardId) {
        baseModel.collapseCard(cardId);
    }

    @Override
    public void selectCard(UUID cardId) {
        baseModel.selectCard(cardId);
    }

    @Override
    public void deselectCard(UUID cardId) {
        baseModel.deselectCard(cardId);
    }

    @Override
    public String getModelLabel() {
        return baseModel.getModelLabel();
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
    public void deleteCard(UUID id) {
        baseModel.deleteCard(id);
    }
}
