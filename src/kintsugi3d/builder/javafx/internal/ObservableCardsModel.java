package kintsugi3d.builder.javafx.internal;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.util.CardSelectionModel;
import kintsugi3d.builder.util.ProjectDataCardFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObservableCardsModel implements CardsModel {
    private final String label;
    private final CardSelectionModel selectedCardsModel;
    private final CardSelectionModel expandedCardsModel;
    private ObservableList<ProjectDataCard> cardsList;
    private ViewSet viewSet;

    public ObservableCardsModel(String label) {
        this.label = label;

        cardsList = FXCollections.observableList(new ArrayList<>());
        selectedCardsModel = new CardSelectionModel(cardsList);
        expandedCardsModel = new CardSelectionModel(cardsList);
    }

    public void setViewSet(ViewSet vset) {
        Platform.runLater(()-> {
            this.viewSet = vset;
            CardsModel model = Global.state().getTabModels().getCardsModel("Cameras");
            List<ProjectDataCard> dataCards = new ArrayList<>();
            for (int i = 0; i < viewSet.getCameraMetadata().size(); i++) {
                String thumbnailPath;
                try {
                    thumbnailPath = viewSet.findThumbnailImageFile(i).toString();
                } catch (FileNotFoundException e) {
                    // Default to icon if thumbnail isn't found
                    thumbnailPath = MainApplication.ICON_PATH;
                }

                dataCards.add(ProjectDataCardFactory.createCameraCard(model, viewSet.getImageFiles().get(i).getName(), thumbnailPath, viewSet.getCameraMetadata().get(i)));
            }
            setCardsList(dataCards);

        });
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
        return expandedCardsModel.getSelectedIds();
    }

    @Override
    public ObservableList<ProjectDataCard> getExpandedCards() {
        return expandedCardsModel.getSelectedItems();
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
        Platform.runLater(()->selectedCardsModel.select(cardId));
    }

    @Override
    public void deselectCard(UUID cardId) {
        Platform.runLater(()->selectedCardsModel.clearSelection(cardId));
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
        Platform.runLater(()->cardsList = items);
    }

    @Override
    public void setCardsList(List<ProjectDataCard> cards) {
        Platform.runLater(()->{
            cardsList.clear();
            cardsList.addAll(cards);
            selectedCardsModel.setCardsList(cardsList);
            expandedCardsModel.setCardsList(cardsList);
        });
    }

    @Override
    public void deleteCard(UUID id) {
        Platform.runLater(()-> {
            if (findIndexByCardUUID(id) != -1 && viewSet != null)
                viewSet.deleteCamera(findIndexByCardUUID(id));

            cardsList.removeIf(card -> card.getCardId().equals(id));
            selectedCardsModel.clearSelection(id);
            expandedCardsModel.clearSelection(id);
        });
    }

    private int findIndexByCardUUID(UUID id) {
        for (int i = 0; i < cardsList.size(); i++) {
            UUID uuid = cardsList.get(i).getCardId();
            if (cardsList.get(i).getCardId() == id) {
                return i;
            }
        }
        return -1;
    }
}
