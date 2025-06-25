package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.javafx.internal.CardsModelImpl;
import kintsugi3d.builder.state.CardsModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SideBarController {

    @FXML
    public RadioButton TexturesButton, CamerasButton;

    @FXML
    public VBox textureTab, cameraTab;

    @FXML public HBox button_box;
    @FXML public VBox main_box;

    public ToggleGroup leftBarTG = new ToggleGroup();
    public List<RadioButton> buttons = new ArrayList<>();
    public List<CardTabController> tabControllers = new ArrayList<>();
    public List<CardsModelImpl> tabModels;


    @FXML
    private void chooseTabAction(ActionEvent event) throws IOException {
//        tabControllers.forEach((controller)-> {
//            controller.setVisible()
//        });
//
//        textureTab.setVisible(TexturesButton.isSelected());
//        textureTab.setManaged(TexturesButton.isSelected());
//        cameraTab.setVisible(CamerasButton.isSelected());
//        cameraTab.setManaged(CamerasButton.isSelected());
    }

    public void init(List<CardsModelImpl> tabModels) {
        this.tabModels = tabModels;
        tabModels.forEach(model -> {
            RadioButton newButton = createButton(model.getLabel());
            VBox newTab = createTab(model);

            button_box.getChildren().add(newButton);
            main_box.getChildren().add(newTab);

            newTab.visibleProperty().bind(newButton.selectedProperty());
            newTab.managedProperty().bind(newButton.selectedProperty());
        });

        buttons.get(0).setSelected(true);
    }

    private RadioButton createButton(String name) {
        RadioButton button = new RadioButton(name);

        // Set sizing
        button.setMinHeight(32.0);
        button.setMaxHeight(32.0);
        button.setPrefHeight(32.0);
        button.setMaxWidth(Double.MAX_VALUE);  // Equivalent to 1.7976931348623157E308

        // Set properties
        button.setMnemonicParsing(false);
        button.setSelected(false);
        button.setStyle("-fx-alignment: center;");
        button.getStyleClass().add("stripped-radio-button");
        button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Add to ToggleGroup
        button.setToggleGroup(leftBarTG);

        // Allow the button to grow horizontally in an HBox
        HBox.setHgrow(button, javafx.scene.layout.Priority.ALWAYS);

        buttons.add(button);

        return button;
    }

    private VBox createTab(CardsModel model) {
        VBox newTab = null;
        CardTabController newTabController = null;
        FXMLLoader loader = new FXMLLoader();
        try {
            loader.setLocation(getClass().getResource("/fxml/menubar/leftpanel/CardTab.fxml"));
            newTab = loader.load();
            newTabController = loader.getController();

            tabControllers.add(newTabController);

            newTabController.init(model);
        } catch (Exception e) {
            // throw new RuntimeException(e);
        }
        return newTab;
    }

}