package kintsugi3d.builder.javafx.internal;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.util.CardSelectionModel;

import java.util.*;

public class CardsModelImpl implements CardsModel {
    private String label;
    private CardSelectionModel selectedCardsModel;
    private CardSelectionModel expandedCardsModel;
    private ObservableList<ProjectDataCard> cardsList;

    public CardsModelImpl(String label) {
//        Platform.runLater(()->{
//            this.label = label;
//            cardsList = new SimpleListProperty<>();
//            selectedCardsModel = new CardSelectionModel(cardsList);
//            expandedCardsModel = new CardSelectionModel(cardsList);
//        });
        this.label = label;
        List<ProjectDataCard> dummyCards = new ArrayList<>();
        dummyCards.add(new ProjectDataCard("Card One", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<>() {{
            put("File Name", "file_one"); put("Resolution", "320x200"); put("Size", "500 KB"); put("Purpose", "This is a description pertaining to the FIRST card."); put("Labels", "");
        }}));

        dummyCards.add(new ProjectDataCard("Card Two", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<>() {{
            put("File Name", "file_two"); put("Resolution", "1080x200"); put("Size", "1000 KB"); put("Purpose", "This is a description pertaining to the SECOND card."); put("Labels", "");
        }}));

        dummyCards.add(new ProjectDataCard("Card Three", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<>() {{
            put("File Name", "file_three"); put("Resolution", "720x200"); put("Size", "1500 KB"); put("Purpose", "This is a description pertaining to the THIRD card."); put("Labels", "");
        }}));

        cardsList = FXCollections.observableList(dummyCards);

        selectedCardsModel = new CardSelectionModel(cardsList);
        expandedCardsModel = new CardSelectionModel(cardsList);
    }

    @Override
    public ObservableList<ProjectDataCard> getSelectedCards() {
        return FXCollections.unmodifiableObservableList(selectedCardsModel.getSelectedItems());
    }

    @Override
    public ObservableSet<UUID> getSelectedCardIds() {
        return FXCollections.unmodifiableObservableSet(selectedCardsModel.getSelectedIds());
    }

    @Override
    public ObservableSet<UUID> getExpandedCardIds() {
        return FXCollections.unmodifiableObservableSet(expandedCardsModel.getSelectedIds());
    }

    @Override
    public ObservableList<ProjectDataCard> getExpandedCards() {
        return FXCollections.unmodifiableObservableList(expandedCardsModel.getSelectedItems());
    }

    @Override
    public void setSelectedCards(List<ProjectDataCard> cards)  {
         Platform.runLater(()->selectedCardsModel.select(cards));
    }

    @Override
    public void setExpandedCards(List<ProjectDataCard> cards) {
        Platform.runLater(()->expandedCardsModel.select(cards));
    }

    @Override
    public UUID getLastSelectedCardId() {
        return selectedCardsModel.getLastSelectedValue();
    }

    @Override
    public UUID getLastExpandedCardId() {
        return expandedCardsModel.getLastSelectedValue();
    }

    @Override
    public ObjectProperty<UUID> getLastSelectedCardProperty() {
        return selectedCardsModel.getLastSelectedProperty();
    }

    @Override
    public ObjectProperty<UUID> getLastExpandedCardProperty() {
        return expandedCardsModel.getLastSelectedProperty();
    }

    @Override
    public boolean isSelected(UUID cardId) {
        return selectedCardsModel.isSelected(cardId);
    }

    @Override
    public boolean isExpanded(UUID cardId) {
        return expandedCardsModel.isSelected(cardId);
    }

    @Override
    public BooleanBinding isSelectedProperty(UUID cardId) {
        return selectedCardsModel.isSelectedProperty(cardId);
    }

    @Override
    public BooleanBinding isExpandedProperty(UUID cardId) {
        return expandedCardsModel.isSelectedProperty(cardId);
    }

    @Override
    public void expandCard(UUID id) {
        Platform.runLater(()->expandedCardsModel.select(id));
    }

    @Override
    public void collapseCard(UUID cardId) {
        Platform.runLater(()->expandedCardsModel.clearSelection(cardId));
    }

    @Override
    public void selectCard(UUID cardId) {
        selectedCardsModel.select(cardId);
    }

    @Override
    public void deselectCard(UUID cardId) {
        selectedCardsModel.clearSelection(cardId);
    }

    @Override
    public String getModelLabel() {
        return label;
    }

    @Override
    public ObservableList<ProjectDataCard> getObservableCardsList() {
        return cardsList;
    }

    @Override
    public void setObservableCardsList(ObservableList<ProjectDataCard> items) {
        Platform.runLater(()->{
            cardsList = items;
        });
    }

    @Override
    public void setCardsList(List<ProjectDataCard> cards) {
        Platform.runLater(()->{
            cardsList.clear();
            cardsList.addAll(cards);

            selectedCardsModel = new CardSelectionModel(cardsList);
            expandedCardsModel = new CardSelectionModel(cardsList);
        });
    }

    @Override
    public void deleteCard(UUID id) {
        cardsList.removeIf(card -> card.getCardId().equals(id));
        selectedCardsModel.clearSelection(id);
        expandedCardsModel.clearSelection(id);
        //Platform.runLater(()->cardsList.removeIf(card -> card.getCardId().equals(id)));
    }
}
