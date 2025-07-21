package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.ArrayList;
import java.util.List;

public class CardTabController {

    @FXML private VBox card_tab;
    @FXML private TextField card_searchbar;
    @FXML private VBox card_vbox;

    private List<VBox> dataCards = new ArrayList<>();
    private List<CardController> cardControllers = new ArrayList<>();

    private CardsModel cardsModel;

    public void init(CardsModel cardsModel)
    {
        this.cardsModel = cardsModel;
        for (ProjectDataCard card : cardsModel.getObservableCardsList()) {
            card_vbox.getChildren().add(createDataCard(card));
        }

        createListener();
    }

    private VBox createDataCard(ProjectDataCard card) {
        VBox newCard = null;
        CardController newCardController;
        FXMLLoader loader = new FXMLLoader();
        try {
            loader.setLocation(getClass().getResource("/fxml/menubar/leftpanel/DataCard.fxml"));
            newCard = loader.load();
            newCardController = loader.getController();

            dataCards.add(newCard);
            cardControllers.add(newCardController);

            newCardController.init(cardsModel, card);
        } catch (Exception e) {
            // throw new RuntimeException(e);
        }
        return newCard;
    }

    @FXML
    public void searchBarAction(KeyEvent e) {
        for (CardController cc : cardControllers) {
            cc.setCardVisibility(cc.titleContainsString(card_searchbar.getText()));
        }
    }

    public void setVisible(boolean visibility) {
        card_tab.setVisible(visibility);
        card_tab.setManaged(visibility);
    }

    //Does not recreate the listener!!!
    public void refreshCardList() {
        card_vbox.getChildren().clear();
        dataCards.clear();
        cardControllers.clear();
        for (ProjectDataCard card : cardsModel.getObservableCardsList()) {
            card_vbox.getChildren().add(createDataCard(card));
        }
    }

    private void createListener() {
        cardsModel.getObservableCardsList().addListener((ListChangeListener<ProjectDataCard>) change -> {
            while(change.next()) {
                if (change.wasRemoved()) {
                    for (int i = 0; i < change.getRemovedSize(); i++) {
                        card_vbox.getChildren().remove(change.getFrom());
                        dataCards.remove(change.getFrom());
                        cardControllers.remove(change.getFrom());
                    }
                }
                if (change.wasAdded()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        card_vbox.getChildren().add(i, createDataCard(cardsModel.getObservableCardsList().get(i)));
                    }
                }
                if(change.wasReplaced()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        card_vbox.getChildren().set(i, createDataCard(cardsModel.getObservableCardsList().get(i)));
                    }
                }
            }
        });
    }
}
