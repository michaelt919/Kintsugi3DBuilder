package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.javafx.internal.CardsModelImpl;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.ArrayList;
import java.util.HashMap;
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
        List<ProjectDataCard> cards = cardsModel.getObservableCardsList();
        for (int i = 0; i < cards.size(); i++) {
            card_vbox.getChildren().add(createDataCard(cards.get(i)));
        }

        cardsModel.getObservableCardsList().addListener(new ListChangeListener<ProjectDataCard>() {
            @Override
            public void onChanged(Change<? extends ProjectDataCard> change) {
                change.next();
                if (change.wasRemoved()) {
                    card_vbox.getChildren().remove(change.getFrom());
                    dataCards.remove(change.getFrom());
                    cardControllers.remove(change.getFrom());
                } else {
                    card_vbox.getChildren().clear();
                    List<ProjectDataCard> cards = cardsModel.getObservableCardsList();
                    for (ProjectDataCard card : cards) {
                        card_vbox.getChildren().add(createDataCard(card));
                    }
                }
            }
        });
    }

    private VBox createDataCard(ProjectDataCard card) {
        VBox newCard = null;
        CardController newCardController = null;
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

    public void refreshCardList() {
        card_vbox.getChildren().clear();
        dataCards.clear();
        cardControllers.clear();
        List<ProjectDataCard> cards = cardsModel.getObservableCardsList();
        for (int i = 0; i < cards.size(); i++) {
            card_vbox.getChildren().add(createDataCard(cards.get(i)));
        }
    }
}
