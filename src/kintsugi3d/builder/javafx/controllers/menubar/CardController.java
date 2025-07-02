package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import javax.script.SimpleBindings;
import java.util.Map;

public class CardController {

    @FXML VBox data_card;
    @FXML VBox card_body;

    @FXML Text card_title;
    @FXML VBox text_content;

    @FXML ImageView card_icon;
    @FXML ImageView main_image;

    private String cardId;

    private CardsModel cameraCardsModel;
    private ProjectDataCard dataCard;


    public void init(CardsModel cameraCardsModel, ProjectDataCard dataCard) {
        this.cameraCardsModel = cameraCardsModel;
        this.dataCard = dataCard;
        this.cardId = dataCard.getCardId();

        card_title.setText(dataCard.getHeaderName());
        //card_icon.setImage(new Image(dataCard.getImagePath()));
        //main_image.setImage(new Image(dataCard.getImagePath()));

        card_body.visibleProperty().bind(cameraCardsModel.isExpandedProperty(cardId));
        card_body.managedProperty().bind(cameraCardsModel.isExpandedProperty(cardId));

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

    public void setCardVisibility(boolean visibility) {
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
    public void headerClicked(MouseEvent e) {
        cameraCardsModel.expandCard(cardId);
    }

    @FXML
    public void expansionToggleClicked(MouseEvent e) {
        if (cameraCardsModel.isExpanded(cardId)) {
            cameraCardsModel.collapseCard(cardId);
        } else {
            cameraCardsModel.expandCard(cardId);
        }
        e.consume();
    }

    @FXML
    public void deleteSelf(ActionEvent e) {
        cameraCardsModel.deleteCard(cardId);
    }

}
