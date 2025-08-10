package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class CardTabController {

    @FXML private VBox card_tab;
    @FXML private TextField card_searchbar;
    @FXML private VBox card_vbox;
    @FXML private ScrollPane scrollpane;

    private double scrollPosition= 0;

    private final ObservableList<CardController> cardControllers = FXCollections.observableArrayList();
    private final FilteredList<CardController> searchList = new FilteredList<>(cardControllers);

    private CardsModel cardsModel;

    public void init(CardsModel cardsModel)
    {
        this.cardsModel = cardsModel;
        for (ProjectDataCard card : cardsModel.getObservableCardsList()) {
            card_vbox.getChildren().add(createDataCard(card).getCard());
        }

        createListeners();
    }

    private CardController createDataCard(ProjectDataCard card) {
        CardController newCardController = null;
        FXMLLoader loader = new FXMLLoader();
        try {
            loader.setLocation(getClass().getResource("/fxml/menubar/leftpanel/DataCard.fxml"));
            loader.load();
            newCardController = loader.getController();
            newCardController.init(cardsModel, card);
            cardControllers.add(newCardController);
        } catch (Exception e) {
            // throw new RuntimeException(e);
        }
        return newCardController;
    }
    // For replace operations
    private CardController createDataCard(ProjectDataCard card, int index) {
        CardController newCardController = null;
        FXMLLoader loader = new FXMLLoader();
        try {
            loader.setLocation(getClass().getResource("/fxml/menubar/leftpanel/DataCard.fxml"));
            loader.load();
            newCardController = loader.getController();
            newCardController.setCardVisibility(false);
            newCardController.init(cardsModel, card);
            cardControllers.set(index, newCardController);
        } catch (Exception e) {
            // throw new RuntimeException(e);
        }
        return newCardController;
    }

    @FXML
    public void searchBarAction(KeyEvent e) {
        /*
        if (card_searchbar.getText().isBlank()) {
            for (int i = 0; i < cardControllers.size(); i++) {
                cardControllers.get(i).setCardVisibility(windowStart < i && i < windowEnd);
            }
        } else {
            for (CardController cc : cardControllers) {
                cc.setCardVisibility(cc.titleContainsString(card_searchbar.getText()));
            }
        }*/
    }

    public void setVisible(boolean visibility) {
        card_tab.setVisible(visibility);
        card_tab.setManaged(visibility);
    }

    //Does not recreate the listener!!!
    public void refreshCardList() {
        card_vbox.getChildren().clear();
        cardControllers.clear();
        for (ProjectDataCard card : cardsModel.getObservableCardsList()) {
            card_vbox.getChildren().add(createDataCard(card).getCard());
        }
    }

    private void createListeners() {
        cardsModel.getObservableCardsList().addListener((ListChangeListener<ProjectDataCard>) change -> {
            while(change.next()) {
                if (change.wasRemoved()) {
                    for (int i = 0; i < change.getRemovedSize(); i++) {
                        cardControllers.remove(change.getFrom());
                    }
                }
                if (change.wasAdded()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        createDataCard(cardsModel.getObservableCardsList().get(i));
                    }
                }
                if(change.wasReplaced()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        createDataCard(cardsModel.getObservableCardsList().get(i), i);
                    }
                }
            }
        });

        searchList.addListener((ListChangeListener<CardController>) change -> {
            while(change.next()) {
                if (change.wasRemoved()) {
                    for (int i = 0; i < change.getRemovedSize(); i++) {
                        card_vbox.getChildren().remove(change.getFrom());
                    }
                }
                if (change.wasAdded()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        CardController cardController = change.getList().get(i);
                        card_vbox.getChildren().add(i, cardController.getCard());
                        // We need to give the UI time to determine the position of its children!
                        Platform.runLater(this::updateViewportVisibility); //
                    }
                }
                if(change.wasReplaced()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        card_vbox.getChildren().set(i, change.getList().get(i).getCard());
                    }
                }
            }
        });

        scrollpane.vvalueProperty().addListener((ChangeListener<? super Number>) (observableValue, oldValue, newValue) -> {
            scrollPosition = newValue.doubleValue();
            updateViewportVisibility();
        });

        card_searchbar.textProperty().addListener((observable, oldValue, newValue) -> {
            searchList.setPredicate(controller -> {
                // If search text is empty, display all items
                return controller.titleContainsString(newValue.toLowerCase());
            });
        });

        // Updates the scrollpane after a datacard is collapsed!!!
        card_vbox.heightProperty().addListener((change)->{
            Platform.runLater(this::updateViewportVisibility);
        });
    }

    public void updateViewportVisibility() {
        double viewportHeight = scrollpane.getViewportBounds().getHeight();
        double contentHeight = card_vbox.getHeight();
        double scrollPointer = scrollPosition * contentHeight;

        for(int i = 0; i < searchList.size(); i++) {
            Bounds cardY = card_vbox.getChildren().get(i).getBoundsInParent();
            boolean inScrollRange = (cardY.getMinY() < scrollPointer + viewportHeight && cardY.getMaxY() > scrollPointer - viewportHeight);
            searchList.get(i).setCardVisibility(inScrollRange);
        }
    }
}


