package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import kintsugi3d.builder.state.CameraCardsModel;

public class CardController {

    @FXML VBox data_card;
    @FXML VBox card_body;

    @FXML Text card_title;
    @FXML Text file_name;
    @FXML Text resolution;
    @FXML Text file_size;
    @FXML Text description;

    private String cardId;
    private int cardIndex;

    private boolean cardVisibility = true;
    private boolean bodyVisibility = false;

    private CameraCardsModel cameraCardsModel;

    public void init(CameraCardsModel cameraCardsModel, int index) {
        this.cameraCardsModel = cameraCardsModel;
        cardIndex = index;
        card_body.visibleProperty().bind(cameraCardsModel.getSelectedCameraViewIndexProperty().isEqualTo(cardIndex));
        card_body.managedProperty().bind(cameraCardsModel.getSelectedCameraViewIndexProperty().isEqualTo(cardIndex));
    }

    @FXML
    public void toggleCardBody(MouseEvent event) {
        bodyVisibility = !bodyVisibility;
        card_body.setVisible(bodyVisibility);
        card_body.setManaged(bodyVisibility);
    }

    public void toggleCardVisibility() {
        cardVisibility = !cardVisibility;
        data_card.setVisible(cardVisibility);
        data_card.setManaged(cardVisibility);
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

    public void setFileName(String fileName) {
        this.file_name.setText(fileName);
    }

    public void setResolution(String resolution) {
        this.resolution.setText(resolution);
    }

    public void setFileSize(String fileSize) {
        this.file_size.setText(fileSize);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

    public String getCardTitle() {
        return this.card_title.getText();
    }

    public boolean titleContainsString(String str) {
        return card_title.getText().contains(str);
    }

    @FXML
    public void selectCard() {
        cameraCardsModel.setSelectedCameraViewIndex(cardIndex);
    }

    @FXML
    public void minimizeCard() {
        if (cameraCardsModel.getSelectedCameraViewIndex() == cardIndex) {
            cameraCardsModel.deselectCard();
        } else {
            cameraCardsModel.setSelectedCameraViewIndex(cardIndex);
        }
    }

    @FXML
    public void deleteSelf(ActionEvent e) {
        cameraCardsModel.deleteCamera(cardIndex);
    }

}
