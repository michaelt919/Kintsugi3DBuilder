package kintsugi3d.builder.javafx.internal;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
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
    private Property<ObservableList<ProjectDataCard>> cardsList;

    public CardsModelImpl(String label) {
        this.label = label;
        List<ProjectDataCard> dummyCards = new ArrayList<>();
        dummyCards.add(new ProjectDataCard("Card One", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<String, String>() {{
            put("File Name", "file_one"); put("Resolution", "320x200"); put("Size", "500 KB"); put("Purpose", "This is a description pertaining to the FIRST card."); put("Labels", "");
        }}));

        dummyCards.add(new ProjectDataCard("Card Two", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<String, String>() {{
            put("File Name", "file_two"); put("Resolution", "1080x200"); put("Size", "1000 KB"); put("Purpose", "This is a description pertaining to the SECOND card."); put("Labels", "");
        }}));

        dummyCards.add(new ProjectDataCard("Card Three", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<String, String>() {{
            put("File Name", "file_three"); put("Resolution", "720x200"); put("Size", "1500 KB"); put("Purpose", "This is a description pertaining to the THIRD card."); put("Labels", "");
        }}));
        cardsList = new SimpleListProperty<>();

        cardsList.setValue(FXCollections.observableList(dummyCards));

        selectedCardsModel = new CardSelectionModel(cardsList);
        expandedCardsModel = new CardSelectionModel(cardsList);
    }

    @Override
    public ObservableList<ProjectDataCard> getSelectedCards() {
        return selectedCardsModel.getSelectedItems();
    }

    @Override
    public ObservableSet<String> getSelectedCardIds() {
        return selectedCardsModel.getSelectedIds();
    }

    @Override
    public ObservableSet<String> getExpandedCardIds() {
        return expandedCardsModel.getSelectedIds();
    }

    @Override
    public ObservableList<ProjectDataCard> getExpandedCards() {
        return expandedCardsModel.getSelectedItems();
    }

    @Override
    public void setSelectedCards(List<ProjectDataCard> cards) {
        selectedCardsModel.select(cards);
    }

    @Override
    public void setExpandedCards(List<ProjectDataCard> cards) {
        expandedCardsModel.select(cards);
    }

    @Override
    public String getLastSelectedCardId() {
        return selectedCardsModel.getLastSelectedValue();
    }

    @Override
    public String getLastExpandedCardId() {
        return expandedCardsModel.getLastSelectedValue();
    }

    @Override
    public StringProperty getLastSelectedCardProperty() {
        return selectedCardsModel.getLastSelectedProperty();
    }

    @Override
    public StringProperty getLastExpandedCardProperty() {
        return expandedCardsModel.getLastSelectedProperty();
    }

    @Override
    public boolean isSelected(String cardId) {
        return selectedCardsModel.isSelected(cardId);
    }

    @Override
    public boolean isExpanded(String cardId) {
        return expandedCardsModel.isSelected(cardId);
    }

    @Override
    public BooleanBinding isSelectedProperty(String cardId) {
        return selectedCardsModel.isSelectedProperty(cardId);
    }

    @Override
    public BooleanBinding isExpandedProperty(String cardId) {
        return expandedCardsModel.isSelectedProperty(cardId);
    }

    @Override
    public void expandCard(String id) {
        expandedCardsModel.select(id);
    }

    @Override
    public void collapseCard(String cardId) {
        expandedCardsModel.clearSelection(cardId);
    }

    @Override
    public void selectCard(String cardId) {
        selectedCardsModel.select(cardId);
    }

    @Override
    public void deselectCard(String cardId) {
        selectedCardsModel.clearSelection(cardId);
    }

    @Override
    public String getModelLabel() {
        return label;
    }

    @Override
    public Property<ObservableList<ProjectDataCard>> getCardListProperty() {
        return cardsList;
    }

    @Override
    public ObservableList<ProjectDataCard> getObservableCardsList() {
        return cardsList.getValue();
    }

    @Override
    public void setObservableCardsList(ObservableList<ProjectDataCard> items) {
        cardsList.setValue(items);
    }

    @Override
    public void setCardsList(List<ProjectDataCard> cards) {
        cardsList.getValue().clear();
        for (ProjectDataCard card: cards) {
            cardsList.getValue().add(card);
        }
        selectedCardsModel = new CardSelectionModel(cardsList);
        expandedCardsModel = new CardSelectionModel(cardsList);
    }

    @Override
    public void deleteCard(String id) {
        cardsList.getValue().removeIf(card -> card.getCardId().equals(id));
    }
}
