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

package kintsugi3d.util;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class RecentProjects {

    private static final File recentProjectsFile = new File(ApplicationFolders.getUserAppDirectory().toFile(), "recentFiles.txt");

    private static final Logger log = LoggerFactory.getLogger(RecentProjects.class);
    private static File recentDirectory;

    private RecentProjects(){throw new IllegalStateException("Utility class");}
    public static List<String> getItemsFromRecentsFile() {
        List<String> projectItems = new ArrayList<>();

        if (recentProjectsFile.exists())
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(recentProjectsFile.getAbsolutePath(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    projectItems.add(line);
                }
            }
            catch (IOException e)
            {
                log.error("Could not get items from recent files list", e);
            }
        }

        //remove duplicates while maintaining the same order (regular HashSet does not maintain order)
        return new ArrayList<>(new LinkedHashSet<>(projectItems));
    }

    public static List<MenuItem> getItemsAsMenuItems(){
        List<String> items = RecentProjects.getItemsFromRecentsFile();

        List<MenuItem> customMenuItems = new ArrayList<>();
        int i = 0;

        //attach tooltips and event handlers
        for (String item : items){
            customMenuItems.add(new MenuItem(shortenedPath(item)));

            MenuItem justAdded = customMenuItems.get(i);

            String fileName = RecentProjects.getItemsFromRecentsFile().get(i);
            Tooltip tooltip = new Tooltip(fileName);
//            Tooltip.install(justAdded.getContent(), tooltip);

            justAdded.setOnAction(event -> {
                ProjectIO.getInstance().openProjectFromFile(new File(fileName));
            });

            ++i;
        }

        return customMenuItems;
    }

    public static String shortenedPath(String path){
        File file = new File(path);
        File ancestorFile = getAncestorFile(file);

        return ancestorFile.getAbsolutePath() + "..." + File.separator + file.getName();
    }

    private static File getAncestorFile(File file) {
        File ancestorFile = file;
        while (ancestorFile.getParentFile()!= null){
            ancestorFile = ancestorFile.getParentFile();
        }
        return ancestorFile;
    }

    public static void addToRecentFiles(String fileName) {
        // Read existing file content into a List
        List<String> existingFileNames = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(recentProjectsFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                existingFileNames.add(line);
            }
        } catch (IOException e) {
            log.error("Failed to update recent files list", e);
        }

        // Check if the fileName is already present
        existingFileNames.remove(fileName); // Remove it from its current position

        // Add the fileName to the front of the List
        existingFileNames.add(0, fileName);

        // Write the updated content back to the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(recentProjectsFile, StandardCharsets.UTF_8))) {
            for (String name : existingFileNames) {
                writer.println(name);
            }
        } catch (IOException e) {
            log.error("Failed to update recent files list", e);
        }

        //update list of recent projects in program
        updateAllControlStructures();
    }

    public static void updateAllControlStructures() {
        if (MenubarController.getInstance()!= null){updateRecentProjectsInMenuBar();}

        if (WelcomeWindowController.getInstance() != null){ updateRecentProjectsInWelcomeWindow();}
    }


    private static void updateRecentProjectsInMenuBar(){
        Menu recentProjsList = MenubarController.getInstance().getRecentProjectsMenu();
        Menu cleanRecentProjectsMenu = MenubarController.getInstance().getCleanRecentProjectsMenu();

        recentProjsList.getItems().clear();

        ArrayList<MenuItem> recentItems = (ArrayList<MenuItem>) RecentProjects.getItemsAsMenuItems();

        recentProjsList.getItems().addAll(recentItems);

        //disable menus if there are no recent projects, otherwise enable
        boolean isListEmpty = recentProjsList.getItems().isEmpty();
        recentProjsList.setDisable(isListEmpty);
        cleanRecentProjectsMenu.setDisable(isListEmpty);
    }

    private static void updateRecentProjectsInWelcomeWindow() {
        WelcomeWindowController welcomeWindowController = WelcomeWindowController.getInstance();

        SplitMenuButton splitMenuButton = welcomeWindowController.recentProjectsSplitMenuButton;
        List<Button> recentButtons = welcomeWindowController.recentButtons;

        ArrayList<MenuItem> recentItems = (ArrayList<MenuItem>)
                RecentProjects.getItemsAsMenuItems();

        splitMenuButton.getItems().clear();
        //disable all quick action buttons then enable them if they hold a project
        for (Button button : recentButtons){
            button.setDisable(true);
            button.setGraphic(null);
            button.setText("");
        }

        //disable split menu button then enable it if it holds projects
        splitMenuButton.setDisable(true);

        int i = 0;
        for (MenuItem item : recentItems) {
            //add first few items to quick access buttons
            if (i < recentButtons.size()){
                Button recentButton = recentButtons.get(i);

                String fileName = RecentProjects.getItemsFromRecentsFile().get(i);
                Tooltip tooltip = new Tooltip(fileName);

                addItemToQuickAccess(fileName, recentButton);
                recentButton.setTooltip(tooltip);
                addContextMenus(recentButton);

                //note: this will still enable the button even if the project does not load properly
                recentButton.setDisable(false);
            }

            //add remaining items under the split menu button
            else{
                splitMenuButton.setDisable(false);
                splitMenuButton.getItems().add(item);
            }

            i++;
        }
    }

    private static void addContextMenus(Labeled control) {
        String path = control.getTooltip().getText();
        String projectName = control.getText();

        ContextMenu contextMenu = new ContextMenu();

        MenuItem remove = new MenuItem("Remove from quick access");
        remove.setOnAction(e -> removeReference(path));
        remove.setStyle("-fx-text-fill: #FFFFFF");

        MenuItem openInExplorer = new MenuItem("Open in file explorer...");
        openInExplorer.setOnAction(e -> {
            File file = new File(path);
            if (!file.exists()){
                ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
                ButtonType removeProj = new ButtonType("Remove from quick access", ButtonBar.ButtonData.YES);

                Alert alert = new Alert(Alert.AlertType.NONE,
                        "Project not found: "+ projectName+ "\n" +
                        "Attempted path: " + path, ok, removeProj);

                ((ButtonBase) alert.getDialogPane().lookupButton(removeProj)).setOnAction(event ->
                        removeReference(path));

                alert.setTitle("Project Not Found");
                alert.show();
                return;
            }

            try{
                //would be nice if this highlighted the file as well, but that (browseFileDirectory()) is not supported :/
                Desktop.getDesktop().open(file.getParentFile());
            }
            catch(IOException ioe){
                ProjectIO.handleException("Failed to open project directory.", ioe);
            }
        });
        openInExplorer.setStyle("-fx-text-fill: #FFFFFF");

        contextMenu.getItems().addAll(remove, openInExplorer);
        control.setOnContextMenuRequested(e -> contextMenu.show(control, e.getScreenX(), e.getScreenY()));
    }



    private static void addItemToQuickAccess(String fileName, Button recentButton) {
        //set project file name
        File projFile = new File(fileName);

        recentButton.setText(projFile.getName());

        //set graphic to ? image if proper thumbnail cannot be found
        recentButton.setGraphic(new ImageView(new Image(new File("question-mark.png").toURI().toString())));
        recentButton.setContentDisplay(ContentDisplay.TOP);

        //get preview image from .k3d file or .ibr file
        setRecentButtonImg(recentButton, projFile);
    }

    private static void setRecentButtonImg(Button recentButton, File projFile) {
        //open file and convert to xml document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            String fullRes = "# Full resolution image file path";
            String prevRes = "# Preview resolution image file path";

            String prevResImgsPath = findImgsPath(factory, projFile, prevRes);
            String fullResImgsPath = findImgsPath(factory, projFile, fullRes);

            if (prevResImgsPath == null && fullResImgsPath == null) {
                log.warn("Could not find preview image for " + projFile.getName());
                return;
            }

            String previewImgPath = null;

            if (prevResImgsPath != null) {
                previewImgPath = getPreviewImgPath(prevResImgsPath, projFile);
            }

            if (previewImgPath == null) {
                //try full imgPath before giving up
                if (fullResImgsPath == null) {
                    log.warn("Could not find preview image for " + projFile.getName());
                    return;
                }

                previewImgPath = getPreviewImgPath(fullResImgsPath, projFile);

                if (previewImgPath == null) {
                    log.warn("Could not find preview image for " + projFile.getName());
                    return;
                }
            }

            ImageView previewImgView = new ImageView(
                    new Image(new File(previewImgPath).toURI().toString(),
                            true));/*enable background loading so we don't freeze the builder*/

            previewImgView.setFitHeight(80);
            previewImgView.setPreserveRatio(true);
            Platform.runLater(()-> recentButton.setGraphic(previewImgView));
        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            log.warn("Could not find preview image for " + projFile.getName(), e);
        }
    }

    private static String getPreviewImgPath(String imgsPath, File projFile) throws IOException {
        //build path off of home directory if path is not complete, otherwise correct path would not be found
        File imgFolder;
        if (!imgsPath.matches("^[A-Za-z]:\\\\.*")){
            String basePath = System.getProperty("user.home");
            File baseDir = new File(basePath);
            imgFolder = new File(baseDir, imgsPath);
        }
        else{
            //full path is given (starting with C:\, G:\, etc)
            imgFolder = new File(imgsPath);
        }

        String canonicalPath = imgFolder.getCanonicalPath();
        File resolvedFile = new File(canonicalPath);

        // Check if the path is a directory
        if (!resolvedFile.isDirectory()) {
            //try again w/ project file parent + imgFolder
            imgFolder = new File(projFile.getParent(), imgsPath);
            canonicalPath = imgFolder.getCanonicalPath();
            resolvedFile = new File(canonicalPath);

            if (!resolvedFile.isDirectory()) {
                //not a warning because we might find the preview image in the other image path
                //first checks preview images, then full res images
                log.info("Could not find preview image for " + projFile.getName() + " in " + resolvedFile.getAbsolutePath());
                return null;
            }
        }

        // List child files
        String[] childFilePaths = resolvedFile.list();

        if (childFilePaths == null || childFilePaths.length == 0) {
            log.warn("No preview images found in " + resolvedFile.getAbsolutePath());
            return null;
        }

        return new File(canonicalPath, childFilePaths[0]).getPath();
    }

    private static String findImgsPath(DocumentBuilderFactory factory, File file, String target) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);

        //get view set path
        Element projectDomElement = (Element) document.getElementsByTagName("Project").item(0);
        Element viewSetDomElement = (Element) projectDomElement.getElementsByTagName("ViewSet").item(0);
        String viewSetPath = new File(file.getParent(), viewSetDomElement.getAttribute("src")).getPath();

        //open images in view set path
        File viewSetFile = new File(viewSetPath);

        Scanner sc = new Scanner(viewSetFile);
        String imgsPath = null;
        String read;
        while (sc.hasNextLine()) {
            read = sc.nextLine();

            //if (read.equals("# Full resolution image file path")){
            //if (read.equals("# Preview resolution image file path")) {
            if (read.equals(target)){
                imgsPath = sc.nextLine();
                //remove the first two chars of the path because it starts with "i "
                imgsPath = imgsPath.substring(2);

                //remove references to parent directories
                String parentPrefix = "..\\";
                String parentPrefixUnix = "../";
                while (imgsPath.startsWith(parentPrefix) || imgsPath.startsWith(parentPrefixUnix)) {
                    imgsPath = imgsPath.substring(parentPrefix.length());
                }
                break;
            }
        }

        return imgsPath;
    }

    private static void removeReference(String fileName) {
        List<String> newRecentItems = getItemsFromRecentsFile().stream()
                .filter(item -> !item.equals(fileName))
                .collect(Collectors.toList());

        // Write the updated content back to the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(recentProjectsFile, StandardCharsets.UTF_8))) {
            for (String name : newRecentItems) {
                writer.println(name);
            }
        } catch (IOException ioe) {
            log.error("Failed to update recent files list while removing invalid reference.", ioe);
        }

        updateAllControlStructures();
    }

    public static void removeInvalidReferences() {
        List<String> newRecentItems = getItemsFromRecentsFile().stream()
                .map(File::new)
                .filter(File::exists)
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        // Write the updated content back to the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(recentProjectsFile, StandardCharsets.UTF_8))) {
            for (String name : newRecentItems) {
                writer.println(name);
            }
        } catch (IOException e) {
            log.error("Failed to update recent files list while removing invalid references.", e);
        }

        updateAllControlStructures();
    }

    public static void removeAllReferences() {
        //wipe recent projects list
        try (FileWriter fileWriter = new FileWriter(recentProjectsFile.getAbsolutePath(), false)) {
            fileWriter.write("");
        } catch (IOException e) {
            log.error("Could not write to recent files list", e);
        }

        updateAllControlStructures();
    }

    public static String getMostRecentProjectPath(){
        return getItemsFromRecentsFile().get(0);
    }

    //use these functions to make file selection more user-friendly across multiple File/Directory Choosers
    public static void setMostRecentDirectory(File file){recentDirectory = file;}
    public static File getMostRecentDirectory(){
        if(recentDirectory != null && recentDirectory.exists()){
            return recentDirectory;
        }

        //loop through recent files and assign/return the first existing one
        for (String path : getItemsFromRecentsFile()){
            File file = new File(path);
            if (file.exists()){
                setMostRecentDirectory(file.getParentFile());
                return getMostRecentDirectory();
            }
        }

        return new File(System.getProperty("user.home"));
    }
}
