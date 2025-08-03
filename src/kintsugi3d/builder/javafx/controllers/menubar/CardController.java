package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.UUID;

public class CardController {

    @FXML VBox data_card;
    @FXML VBox card_body;
    @FXML VBox border_box;

    @FXML Text card_title;
    @FXML VBox text_content;

    @FXML ImageView card_icon;
    @FXML ImageView main_image;

    @FXML VBox button_box;

    private UUID cardId;
    private CardsModel cameraCardsModel;
    private ProjectDataCard dataCard;
    private BooleanBinding expanded;
    private BooleanBinding selected;
    private Image preview = null;

    public void init(CardsModel cameraCardsModel, ProjectDataCard dataCard) {
        this.cameraCardsModel = cameraCardsModel;
        this.dataCard = dataCard;
        this.cardId = dataCard.getCardId();

        card_title.setText(dataCard.getTitle());
        try {
//            Image preview = new Image(dataCard.getImagePath());
//            card_icon.setImage(preview);
//            main_image.setImage(preview);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        expanded = cameraCardsModel.isExpandedProperty(cardId);
        selected = cameraCardsModel.isSelectedProperty(cardId);

        card_body.visibleProperty().bind(expanded);
        card_body.managedProperty().bind(expanded);
        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                border_box.setStyle("-fx-border-color: black; -fx-border-width: 2px;");
                data_card.setStyle("-fx-padding: 2px;");
            } else {
                border_box.setStyle("");
                data_card.setStyle("-fx-padding: 4px");
            }
        });

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

        button_box.getChildren().clear();
        dataCard.getActions().forEach((group)-> {
            Separator separator = new Separator();
            separator.setPrefWidth(200.0);
            separator.getStyleClass().add("card-separator");
            separator.setPadding(new Insets(16.0, 8.0, 16, 8.0)); // Top, Right, Bottom, Left
            button_box.getChildren().add(separator);
            group.forEach((label, action)-> {
                // Create the HBox
                HBox hBox = new HBox();
                hBox.setAlignment(Pos.TOP_CENTER);

//                // Create the ImageView for the graphic
//                Image image = new Image("file:./Kintsugi3D-icon.png"); // Assuming the image is in the project root
//                ImageView imageView = new ImageView(image);
//                imageView.setFitHeight(16.0);
//                imageView.setFitWidth(16.0);
//                imageView.setPickOnBounds(true);
//                imageView.setPreserveRatio(true);

                // Create the Button
                Button button = new Button(label);
                button.setGraphicTextGap(8.0);
                button.setMnemonicParsing(false);
                button.setStyle("-fx-text-fill: #CECECE;");
                button.getStyleClass().add("card-button"); // Assuming this style class is defined in the CSS
                button.getStylesheets().add("file:./kintsugiStyling.css"); // Load the stylesheet
                button.setOnAction(event->action.run());

                // Set the font for the button text
                button.setFont(new Font("Segoe UI Semibold", 12.0));

                // Set the margin for the button within the HBox
                HBox.setMargin(button, new Insets(0, 0, 8, 0));

                // Set the padding for the HBox
                hBox.setPadding(new Insets(0, 40.0, 0, 40.0));

                // Add the button to the HBox
                hBox.getChildren().add(button);

                button_box.getChildren().add(hBox);
            });
        });


        card_body.visibleProperty().addListener((change, oldVal, newVal) -> {
            if (preview == null && newVal == true) {
                preview = new Image(dataCard.getImagePath());
                card_icon.setImage(preview);
                main_image.setImage(preview);
            }
        });
    }

    public void setCardVisibility(boolean visibility) {
        data_card.setVisible(visibility);
    }

    public void setCardId(UUID uuid) {
        this.cardId = uuid;
    }

    public void setCardTitle(String title) {
        this.card_title.setText(title);
    }

    public String getCardTitle() {
        return this.card_title.getText();
    }

    public UUID getCardId() {
        return this.cardId;
    }

    public boolean titleContainsString(String str) {
        return card_title.getText().toLowerCase().contains(str.toLowerCase());
    }

    @FXML
    public void cardClicked(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            if (cameraCardsModel.isSelected(cardId)){
                cameraCardsModel.deselectCard(cardId);
            }else {
                cameraCardsModel.selectCard(cardId);
            }
        } else if (e.getButton() == MouseButton.SECONDARY) {
            if (cameraCardsModel.isExpanded(cardId)) {
                cameraCardsModel.collapseCard(cardId);
            } else {
                cameraCardsModel.expandCard(cardId);
            }
        }
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

    public VBox getCard() {
        return data_card;
    }
}
