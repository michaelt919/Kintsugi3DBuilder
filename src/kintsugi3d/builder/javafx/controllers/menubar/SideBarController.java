package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.TabModels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SideBarController {
    @FXML public HBox button_box;
    @FXML public VBox main_box;

    public ToggleGroup leftBarTG = new ToggleGroup();
    public List<RadioButton> buttons = new ArrayList<>();
    public List<CardTabController> tabControllers = new ArrayList<>();
    public TabModels tabModels;

    public void init(TabModels tabModels) {
        this.tabModels = tabModels;
        tabModels.getAllCardsModels().forEach(model -> {
            RadioButton newButton = createButton(model.getModelLabel());
            VBox newTab = createTab(model);

            button_box.getChildren().add(newButton);
            main_box.getChildren().add(newTab);

            newTab.visibleProperty().bind(newButton.selectedProperty());
            newTab.managedProperty().bind(newButton.selectedProperty());
        });

        tabModels.getItems().addListener((MapChangeListener<String, CardsModel>) change -> {
            //unnecessary?
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

    public void refreshTabs() {
        tabControllers.forEach(CardTabController::refreshCardList);
    }

}