package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

public class CardController {

    @FXML VBox data_card;
    @FXML VBox card_body;

    @FXML Text card_title;
    @FXML VBox text_content;

    @FXML ImageView card_icon;
    @FXML ImageView main_image;

    private String cardId;
    private int cardIndex;

    private boolean cardVisibility = true;
    private boolean bodyVisibility = false;

    private CardsModel cameraCardsModel;
    private ProjectDataCard dataCard;


    public void init(CardsModel cameraCardsModel, ProjectDataCard dataCard, int index) {
        this.cameraCardsModel = cameraCardsModel;
        this.dataCard = dataCard;
        this.cardIndex = index;

        card_title.setText(dataCard.getHeaderName());
        //card_icon.setImage(new Image(dataCard.getImagePath()));
        //main_image.setImage(new Image(dataCard.getImagePath()));

        card_body.visibleProperty().bind(cameraCardsModel.getSelectedCardIndexProperty().isEqualTo(cardIndex));
        card_body.managedProperty().bind(cameraCardsModel.getSelectedCardIndexProperty().isEqualTo(cardIndex));

        text_content.getChildren().clear();
        dataCard.getTextContent().forEach((key, value) -> {
                Text label = new Text(key + ":");
                label.setFont(Font.font("Segoe UI Semibold", 12));

                Text caption = new Text(value);
                caption.getStyleClass().add("wireframeCaption");
                TextFlow flow = new TextFlow(caption);
                flow.setPrefWidth(200);

                text_content.getChildren().add(label);
                text_content.getChildren().add(flow);
                VBox.setMargin(flow, new Insets(0,0,4,5));
        });
    }

    @FXML
    public void toggleCardBody(MouseEvent event) {
        bodyVisibility = !bodyVisibility;
        card_body.setVisible(bodyVisibility);
        card_body.setManaged(bodyVisibility);
    }

    public void setCardVisibility(boolean visibility) {
        cardVisibility = visibility;
        data_card.setVisible(visibility);
        data_card.setManaged(visibility);
    }

    public void setCardId(String uuid) {
        this.cardId = uuid;
    }

    public void setCardTitle(String title) {
        this.card_title.setText(title);
    }

    public String getCardTitle() {
        return this.card_title.getText();
    }

    public boolean titleContainsString(String str) {
        return card_title.getText().toLowerCase().contains(str.toLowerCase());
    }

    @FXML
    public void selectCard() {
        cameraCardsModel.setSelectedCardIndex(cardIndex);
    }

    @FXML
    public void minimizeCard() {
        if (cameraCardsModel.getSelectedCardIndex() == cardIndex) {
            cameraCardsModel.deselectCard();
        } else {
            cameraCardsModel.setSelectedCardIndex(cardIndex);
        }
    }

    @FXML
    public void deleteSelf(ActionEvent e) {
        cameraCardsModel.deleteCard(cardIndex);
    }

}
