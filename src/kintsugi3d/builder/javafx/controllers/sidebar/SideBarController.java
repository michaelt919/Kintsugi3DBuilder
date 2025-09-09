package kintsugi3d.builder.javafx.controllers.sidebar;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.javafx.internal.ObservableTabsModel;
import kintsugi3d.builder.state.CardsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SideBarController
{
    private static final Logger LOG = LoggerFactory.getLogger(SideBarController.class);

    @FXML private HBox buttonBox;
    @FXML private VBox mainBox;

    private final ToggleGroup tabToggleGroup = new ToggleGroup();
    private final List<RadioButton> buttons = new ArrayList<>(8);
    private final Collection<CardTabController> tabControllers = new ArrayList<>(4);

    public Node getRootNode()
    {
        return mainBox;
    }

    public void init(ObservableTabsModel tabModels)
    {
        tabModels.getAllTabs().forEach(model ->
        {
            RadioButton newButton = createButton(model.getModelLabel());
            VBox newTab = createTab(model);

            buttonBox.getChildren().add(newButton);
            mainBox.getChildren().add(newTab);

            newTab.visibleProperty().bind(newButton.selectedProperty());
            newTab.managedProperty().bind(newButton.selectedProperty());
        });

        tabModels.getObservableTabsMap().addListener((MapChangeListener<String, CardsModel>) change ->
        {
            if (change.wasAdded())
            {
                CardsModel model = change.getValueAdded();
                RadioButton newButton = createButton(model.getModelLabel());
                VBox newTab = createTab(model);

                buttonBox.getChildren().add(newButton);
                mainBox.getChildren().add(newTab);

                newTab.visibleProperty().bind(newButton.selectedProperty());
                newTab.managedProperty().bind(newButton.selectedProperty());
            }

            if (change.wasRemoved())
            {
                LOG.warn("Removing tabs is not currently supported.  Unexpected behavior may occur.");
            }

            // Refresh whether tabs are visible any time the tabs model is updated.
            if (tabModels.getAllTabs().size() < 2)
            {
                buttonBox.setVisible(false);
                buttonBox.setManaged(false);
            }
            else
            {
                buttonBox.setVisible(true);
                buttonBox.setManaged(true);
            }

            // Select the first tab if no tab is selected.
            if (!tabModels.getAllTabs().isEmpty() && tabToggleGroup.getSelectedToggle() == null)
            {
                buttons.get(0).setSelected(true);
            }
        });
    }

    private RadioButton createButton(String name)
    {
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
        button.setToggleGroup(tabToggleGroup);

        // Allow the button to grow horizontally in an HBox
        HBox.setHgrow(button, javafx.scene.layout.Priority.ALWAYS);

        buttons.add(button);

        return button;
    }

    private VBox createTab(CardsModel model)
    {
        VBox newTab = null;
        FXMLLoader loader = new FXMLLoader();
        try
        {
            loader.setLocation(getClass().getResource("/fxml/main/leftpanel/CardTab.fxml"));
            newTab = loader.load();
            CardTabController newTabController = loader.getController();

            tabControllers.add(newTabController);

            newTabController.init(model);
        }
        catch (IOException e)
        {
            // throw new RuntimeException(e);
        }
        return newTab;
    }

    public void setVisibility(boolean visible)
    {
        mainBox.setVisible(visible);
        mainBox.setManaged(visible);
        if (visible)
        {
            Platform.runLater(() -> tabControllers.forEach(CardTabController::updateViewportVisibility));
        }
    }

    public void refreshTabs()
    {
        tabControllers.forEach(CardTabController::refreshCardList);
    }

}