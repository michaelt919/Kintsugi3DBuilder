/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.core;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.io.RecentProjects;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WelcomeWindowController
{
    private static final Logger LOG = LoggerFactory.getLogger(WelcomeWindowController.class);

    private static WelcomeWindowController instance;

    static WelcomeWindowController getInstance()
    {
        return instance;
    }

    @FXML private Button recent1;
    @FXML private Button recent2;
    @FXML private Button recent3;
    @FXML private Button recent4;
    @FXML private Button recent5;

    @FXML private SplitMenuButton recentProjectsSplitMenuButton;

    private final List<Button> recentButtons = new ArrayList<>(16);

    private Stage window;
    private Window parentWindow;

    private Runnable userDocumentationHandler;

    private BooleanExpression shouldBeHidden;

    public void init(Stage injectedStage, JavaFXState state, Runnable injectedUserDocumentationHandler)
    {
        instance = this;

        this.parentWindow = injectedStage.getOwner();
        this.window = injectedStage;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        recentButtons.add(recent1);
        recentButtons.add(recent2);
        recentButtons.add(recent3);
        recentButtons.add(recent4);
        recentButtons.add(recent5);

        RecentProjectsHelper.updateAllControlStructures();

        shouldBeHidden = ExperienceManager.getInstance().getAnyModalOpenProperty()
            .or(ProgressBarsController.getInstance().getProcessingProperty())
            .or(JavaFXState.getInstance().getProjectModel().getProjectOpenProperty());

        InvalidationListener windowHide = obs ->
            // Delay to allow it to catch if the main window is being closed.
            Platform.runLater(() ->
            {
                if (parentWindow.isShowing()) // Try to prevent race condition when closing main window.
                {
                    if (shouldBeHidden.get())
                    {
                        window.hide();
                    }
                    else
                    {
                        window.show();
                    }
                }
            });
        shouldBeHidden.addListener(windowHide);

        // Try to prevent race condition when closing main window.
        parentWindow.showingProperty().addListener(obs ->
        {
            if (!parentWindow.isShowing())
            {
                shouldBeHidden.removeListener(windowHide);
            }
        });

        LOG.info("Checking for cache cleanup...");

        // Prompt user to clear cache if conditions are met
        state.getCacheModel().requestPromptForCacheCleanup(
            promptCacheSize ->
            {
                LOG.info("Cache size: {}GB", promptCacheSize);

                Alert prompt = new Alert(AlertType.CONFIRMATION);
                prompt.setTitle("Clean Cache?");
                prompt.setHeaderText("Clean up old cache files?");
                prompt.setContentText(String.format("The cache for Kintsugi 3D Builder contains older files that can be removed to free up disk space. Currently, the total size of the cache is %.2fGB. Would you like to clean up the cache by removing older files?",
                    promptCacheSize));

                prompt.showAndWait().ifPresent(response ->
                {
                    if (response.equals(ButtonType.OK))
                    {
                        state.getCacheModel().checkForCleanUpCachePrompt();
                    }
                });
            },
            promptCacheSize -> LOG.info("No cache cleanup needed (Size: {}GB)", promptCacheSize));
    }

    /**
     * Useful for showing alerts that would otherwise be covered by up the welcome window
     * (and by extension, the main window as a parent of the welcome window).
     * @param runnable
     */
    public void runOnceWhenShown(Runnable runnable)
    {
        ReadOnlyBooleanProperty showing = window.showingProperty();

        ChangeListener<Boolean> runOnce = new ChangeListener<>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean wasShown, Boolean isShown)
            {
                if (isShown)
                {
                    runnable.run();
                    showing.removeListener(this);
                }
            }
        };

        showing.addListener(runOnce);
    }

    private static void handleMenuItemSelection(MenuItem item)
    {
        String projectName = item.getText();
        FrontendIO.openProjectFromFile(new File(projectName));
    }

    public void splitMenuButtonActions(ActionEvent actionEvent)
    {
        Object source = actionEvent.getSource();
        //user clicks on a menu item
        if (source.getClass().equals(MenuItem.class))
        {
            handleMenuItemSelection((MenuItem) actionEvent.getSource());
        }
        //user clicks on the button, so unroll the menu
        else
        {
            unrollMenu();
        }
    }

    public void createProject()
    {
        if (!FrontendIO.isCreateProjectWindowOpen())
        {
            FrontendIO.createProject(parentWindow);
        }
    }

    @FXML
    private void openProject()
    {
        FrontendIO.getInstance().openProject(parentWindow);
    }

    @FXML
    private void help_userManual()
    {
        userDocumentationHandler.run();
    }

    private void unrollMenu()
    {
        recentProjectsSplitMenuButton.show();
    }

    public void hideMenu()
    {
        //recentProjectsSplitMenuButton.hide();
        //TODO: ONLY HIDE THE MENU WHEN THE USER'S MOUSE LEAVES THE CONTEXT MENU
    }

    public void recentButton(ActionEvent actionEvent)
    {
        Object source = actionEvent.getSource();
        //user clicks on a menu item
        if (source.getClass() == Button.class)
        {
            handleButtonSelection((Button) actionEvent.getSource());
        }
    }

    private void handleButtonSelection(Button item)
    {
        ArrayList<String> recentFileNames = (ArrayList<String>) RecentProjects.getRecentProjectFilenames();
        int i = 0;
        for (Button button : recentButtons)
        {
            if (Objects.equals(button, item))
            {
                FrontendIO.openProjectFromFile(new File(recentFileNames.get(i)));
            }
            i++;
        }
    }

    @FXML
    public void openSystemSettingsModal()
    {
        ExperienceManager.getInstance().getExperience("SystemSettings").tryOpen();
    }

    @FXML
    public void openAboutModal()
    {
        ExperienceManager.getInstance().getExperience("About").tryOpen();
    }

    public void updateRecentProjects()
    {
        List<String> items = RecentProjects.getRecentProjectFilenames();
        List<MenuItem> recentItems = RecentProjectsHelper.getMenuItems(items);

        recentProjectsSplitMenuButton.getItems().clear();
        //disable all quick action buttons then enable them if they hold a project
        for (Button button : recentButtons)
        {
            button.setDisable(true);
            button.setGraphic(null);
            button.setText("");
        }

        //disable split menu button then enable it if it holds projects
        recentProjectsSplitMenuButton.setDisable(true);

        int i = 0;
        for (MenuItem item : recentItems)
        {
            //add first few items to quick access buttons
            if (i < recentButtons.size())
            {
                Button recentButton = recentButtons.get(i);

                String fileName = items.get(i);
                Tooltip tooltip = new Tooltip(fileName);

                addItemToQuickAccess(fileName, recentButton);
                recentButton.setTooltip(tooltip);
                addContextMenus(recentButton);

                //note: this will still enable the button even if the project does not load properly
                recentButton.setDisable(false);
            }

            //add remaining items under the split menu button
            else
            {
                recentProjectsSplitMenuButton.setDisable(false);
                recentProjectsSplitMenuButton.getItems().add(item);
            }

            i++;
        }
    }

    private static void addContextMenus(Labeled control)
    {
        String path = control.getTooltip().getText();
        String projectName = control.getText();

        ContextMenu contextMenu = new ContextMenu();

        MenuItem remove = new MenuItem("Remove from quick access");
        remove.setOnAction(e -> removeReference(path));
        remove.setStyle("-fx-text-fill: #FFFFFF");

        MenuItem openInExplorer = new MenuItem("Open in file explorer...");
        openInExplorer.setOnAction(e ->
        {
            File file = new File(path);
            if (!file.exists())
            {
                ButtonType ok = new ButtonType("OK", ButtonData.CANCEL_CLOSE);
                ButtonType removeProj = new ButtonType("Remove from quick access", ButtonData.YES);

                Alert alert = new Alert(AlertType.NONE,
                    String.format("Project not found: %s\nAttempted path: %s", projectName, path), ok, removeProj);

                ((ButtonBase) alert.getDialogPane().lookupButton(removeProj)).setOnAction(event ->
                    removeReference(path));

                alert.setTitle("Project Not Found");
                alert.show();
                return;
            }

            try
            {
                //would be nice if this highlighted the file as well, but that (browseFileDirectory()) is not supported :/
                Desktop.getDesktop().open(file.getParentFile());
            }
            catch (IOException ioe)
            {
                ExceptionHandling.error("Failed to open project directory", ioe);
            }
        });
        openInExplorer.setStyle("-fx-text-fill: #FFFFFF");

        contextMenu.getItems().addAll(remove, openInExplorer);
        control.setOnContextMenuRequested(e -> contextMenu.show(control, e.getScreenX(), e.getScreenY()));
    }

    private static void removeReference(String fileName)
    {
        List<String> newRecentItems = RecentProjects.getRecentProjectFilenames().stream()
            .filter(item -> !item.equals(fileName))
            .collect(Collectors.toList());

        // Write the updated content back to the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(RecentProjects.RECENT_PROJECTS_FILE, StandardCharsets.UTF_8)))
        {
            for (String name : newRecentItems)
            {
                writer.println(name);
            }
        }
        catch (IOException ioe)
        {
            LOG.error("Failed to update recent files list while removing invalid reference.", ioe);
        }

        RecentProjectsHelper.updateAllControlStructures();
    }

    private static void addItemToQuickAccess(String fileName, Button recentButton)
    {
        //set project file name
        File projFile = new File(fileName);

        recentButton.setText(projFile.getName());

        //set graphic to ? image if proper thumbnail cannot be found
        recentButton.setGraphic(new ImageView(new Image(new File("question-mark.png").toURI().toString())));
        recentButton.setContentDisplay(ContentDisplay.TOP);

        //get preview image from .k3d file or .ibr file
        setRecentButtonImg(recentButton, projFile);
    }

    private static void setRecentButtonImg(Button recentButton, File projFile)
    {
        try
        {
            File vsetFile = Global.state().getProjectModel().getViewSetFileForProject(projFile);
            ViewSet viewSet = ViewSetReaderFromVSET.getInstance().readFromFile(vsetFile)
                .finish();
            File previewImageFile = viewSet.getRepresentativeView().getPreviewImageFile();

            ImageView previewImgView = new ImageView(
                new Image(previewImageFile.toURI().toString(),
                    true)); /* enable background loading so we don't freeze the builder */

            previewImgView.setFitHeight(80);
            previewImgView.setPreserveRatio(true);
            Platform.runLater(() -> recentButton.setGraphic(previewImgView));
        }
        catch (IOException | ParserConfigurationException | SAXException e)
        {
            LOG.warn("Could not find preview image for {}", projFile.getName(), e);
        }
    }
}
