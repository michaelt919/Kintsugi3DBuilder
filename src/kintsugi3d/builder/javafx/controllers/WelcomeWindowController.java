/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
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
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.experience.ExperienceManager;
import kintsugi3d.builder.javafx.util.ExceptionHandling;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class WelcomeWindowController
{
    private static final Logger LOG = LoggerFactory.getLogger(WelcomeWindowController.class);

    private static WelcomeWindowController INSTANCE;

    public static WelcomeWindowController getInstance()
    {
        return INSTANCE;
    }

    @FXML private Button recent1;
    @FXML private Button recent2;
    @FXML private Button recent3;
    @FXML private Button recent4;
    @FXML private Button recent5;

    @FXML private SplitMenuButton recentProjectsSplitMenuButton;

    private final List<Button> recentButtons = new ArrayList<>();

    private Stage window;
    private Window parentWindow;

    private Runnable userDocumentationHandler;

    private BooleanExpression shouldBeHidden;

    public void init(Stage injectedStage, Runnable injectedUserDocumentationHandler)
    {
        INSTANCE = this;

        this.parentWindow = injectedStage.getOwner();
        this.window = injectedStage;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        recentButtons.add(recent1);
        recentButtons.add(recent2);
        recentButtons.add(recent3);
        recentButtons.add(recent4);
        recentButtons.add(recent5);

        RecentProjects.updateAllControlStructures();

        shouldBeHidden = ExperienceManager.getInstance().getAnyModalOpenProperty()
            .or(ProgressBarsController.getInstance().getProcessingProperty())
            .or(ProjectIO.getInstance().getProjectLoadedProperty());

        shouldBeHidden.addListener(obs ->
        {
            if (shouldBeHidden.get())
            {
                window.hide();
            }
            else
            {
                window.show();
            }
        });
    }

    private static void handleMenuItemSelection(MenuItem item)
    {
        String projectName = item.getText();
        ProjectIO.getInstance().openProjectFromFile(new File(projectName));
    }

    public void splitMenuButtonActions(ActionEvent actionEvent)
    {
        Object source = actionEvent.getSource();
        //user clicks on a menu item
        if (source.getClass() == MenuItem.class)
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
        if (!ProjectIO.getInstance().isCreateProjectWindowOpen())
        {
            ProjectIO.getInstance().createProject(parentWindow);
        }
    }

    @FXML
    private void openProject()
    {
        ProjectIO.getInstance().openProjectWithPrompt(parentWindow);
    }

    @FXML
    private void help_userManual()
    {
        userDocumentationHandler.run();
    }

    public void unrollMenu()
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

    public void handleButtonSelection(Button item)
    {
        ArrayList<String> recentFileNames = (ArrayList<String>) RecentProjects.getItemsFromRecentsFile();
        int i = 0;
        for (Button button : recentButtons)
        {
            if (button == item)
            {
                ProjectIO.getInstance().openProjectFromFile(new File(recentFileNames.get(i)));
            }
            i++;
        }
    }

    public void openSystemSettingsModal()
    {
        ExperienceManager.getInstance().getExperience("SystemSettings").tryOpen();
    }

    public void openAboutModal()
    {
        ExperienceManager.getInstance().getExperience("About").tryOpen();
    }

    public void updateRecentProjects()
    {
        List<String> items = RecentProjects.getItemsFromRecentsFile();
        List<CustomMenuItem> recentItems = RecentProjects.getItemsAsCustomMenuItems(items);

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
        for (CustomMenuItem item : recentItems)
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
                ExceptionHandling.error("Failed to open project directory.", ioe);
            }
        });
        openInExplorer.setStyle("-fx-text-fill: #FFFFFF");

        contextMenu.getItems().addAll(remove, openInExplorer);
        control.setOnContextMenuRequested(e -> contextMenu.show(control, e.getScreenX(), e.getScreenY()));
    }

    private static void removeReference(String fileName)
    {
        List<String> newRecentItems = RecentProjects.getItemsFromRecentsFile().stream()
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

        RecentProjects.updateAllControlStructures();
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
        //open file and convert to xml document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try
        {
            String fullRes = "# Full resolution image file path";
            String prevRes = "# Preview resolution image file path";

            String prevResImgsPath = findImgsPath(factory, projFile, prevRes);
            String fullResImgsPath = findImgsPath(factory, projFile, fullRes);

            if ((prevResImgsPath == null) && (fullResImgsPath == null))
            {
                LOG.warn("Could not find preview image for {}", projFile.getName());
                return;
            }

            String previewImgPath = null;

            if (prevResImgsPath != null)
            {
                previewImgPath = getPreviewImgPath(prevResImgsPath, projFile);
            }

            if (previewImgPath == null)
            {
                //try full imgPath before giving up
                if (fullResImgsPath == null)
                {
                    LOG.warn("Could not find preview image for {}", projFile.getName());
                    return;
                }

                previewImgPath = getPreviewImgPath(fullResImgsPath, projFile);

                if (previewImgPath == null)
                {
                    LOG.warn("Could not find preview image for {}", projFile.getName());
                    return;
                }
            }

            ImageView previewImgView = new ImageView(
                new Image(new File(previewImgPath).toURI().toString(),
                    true));/*enable background loading so we don't freeze the builder*/

            previewImgView.setFitHeight(80);
            previewImgView.setPreserveRatio(true);
            Platform.runLater(() -> recentButton.setGraphic(previewImgView));
        }
        catch (ParserConfigurationException | IOException | SAXException e)
        {
            LOG.warn("Could not find preview image for {}", projFile.getName(), e);
        }
    }

    private static String findImgsPath(DocumentBuilderFactory factory, File file, String target) throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);

        //get view set path
        Element projectDomElement = (Element) document.getElementsByTagName("Project").item(0);
        Element viewSetDomElement = (Element) projectDomElement.getElementsByTagName("ViewSet").item(0);
        String viewSetPath = new File(file.getParent(), viewSetDomElement.getAttribute("src")).getPath();

        //open images in view set path
        File viewSetFile = new File(viewSetPath);

        try (Scanner sc = new Scanner(viewSetFile, StandardCharsets.UTF_8))
        {
            while (sc.hasNextLine())
            {
                String read = sc.nextLine();

                //if (read.equals("# Full resolution image file path")){
                //if (read.equals("# Preview resolution image file path")) {
                if (read.equals(target))
                {
                    String imgsPath = sc.nextLine();
                    //remove the first two chars of the path because it starts with "i "
                    imgsPath = imgsPath.substring(2);

                    //remove references to parent directories
                    String parentPrefix = "..\\";
                    String parentPrefixUnix = "../";
                    while (imgsPath.startsWith(parentPrefix) || imgsPath.startsWith(parentPrefixUnix))
                    {
                        imgsPath = imgsPath.substring(parentPrefix.length());
                    }
                    return imgsPath;
                }
            }

            return null;
        }
    }

    private static String getPreviewImgPath(String imgsPath, File projFile) throws IOException
    {
        //build path off of home directory if path is not complete, otherwise correct path would not be found
        File imgFolder;
        if (imgsPath.matches("^[A-Za-z]:\\\\.*"))
        {
            //full path is given (starting with C:\, G:\, etc)
            imgFolder = new File(imgsPath);
        }
        else
        {
            String basePath = System.getProperty("user.home");
            File baseDir = new File(basePath);
            imgFolder = new File(baseDir, imgsPath);
        }

        String canonicalPath = imgFolder.getCanonicalPath();
        File resolvedFile = new File(canonicalPath);

        // Check if the path is a directory
        if (!resolvedFile.isDirectory())
        {
            //try again w/ project file parent + imgFolder
            imgFolder = new File(projFile.getParent(), imgsPath);
            canonicalPath = imgFolder.getCanonicalPath();
            resolvedFile = new File(canonicalPath);

            if (!resolvedFile.isDirectory())
            {
                //not a warning because we might find the preview image in the other image path
                //first checks preview images, then full res images
                LOG.info("Could not find preview image for {} in {}", projFile.getName(), resolvedFile.getAbsolutePath());
                return null;
            }
        }

        // List child files
        String[] childFilePaths = resolvedFile.list();

        if ((childFilePaths == null) || (childFilePaths.length == 0))
        {
            LOG.warn("No preview images found in {}", resolvedFile.getAbsolutePath());
            return null;
        }

        return new File(canonicalPath, childFilePaths[0]).getPath();
    }
}
