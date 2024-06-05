/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.util;

import javafx.scene.control.*;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;

public class RecentProjects {

    private static WelcomeWindowController welcomeWindowController;
    private static File recentProjectsFile = new File(ApplicationFolders.getUserAppDirectory().toFile(), "recentFiles.txt");

    private static final Logger log = LoggerFactory.getLogger(RecentProjects.class);

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
                    String projectItem = line;
                    projectItems.add(projectItem);
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

        List<MenuItem> menuItems = new ArrayList<>();
        for (String item : items){
            menuItems.add(new MenuItem(shortenedPath(item)));
        }

        return menuItems;
    }

    public static String shortenedPath(String path){
        File file = new File(path);
        File ancestorFile = getAncestorFile(file);

        return ancestorFile.getAbsolutePath() + "...\\" + file.getName();
    }

    private static File getAncestorFile(File file) {
        File ancestorFile = file;
        while (ancestorFile.getParentFile()!= null){
            ancestorFile = ancestorFile.getParentFile();
        }
        return ancestorFile;
    }

    public static void updateRecentFiles(String fileName) {
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
        updateRecentProjectsInWelcomeWindow();
        MenubarController.getInstance().updateRecentProjectsMenu();
    }

    public static void initializeWelcomeWindowController(WelcomeWindowController welcomeWindowController) {
        RecentProjects.welcomeWindowController = welcomeWindowController;
    }

    public static void updateRecentProjectsInMenuBar(Menu menu){
        menu.getItems().clear();

        ArrayList<MenuItem> recentItems = (ArrayList<MenuItem>) RecentProjects.getItemsAsMenuItems();

        menu.getItems().addAll(recentItems);

        //disable button if there are no recent projects, otherwise enable
        menu.setDisable(menu.getItems().isEmpty());

        //attach event handlers to all menu items
        for (MenuItem item : recentItems) {
            item.setOnAction(event -> handleMenuItemSelection(item));
        }
    }

    public static void updateRecentProjectsInWelcomeWindow() {

        SplitMenuButton splitMenuButton = welcomeWindowController.recentProjectsSplitMenuButton;
        ArrayList<Button> recentButtons = welcomeWindowController.recentButtons;

        ArrayList<MenuItem> recentItems = (ArrayList<MenuItem>) RecentProjects.getItemsAsMenuItems();

        splitMenuButton.getItems().clear();
        //disable all quick action buttons then enable them if they hold a project
        for (Button button : recentButtons){
            button.setDisable(true);
            button.setGraphic(null);
            button.setText("");
        }

        //disable split menu button and enable it if it holds projects
        splitMenuButton.setDisable(true);

        //attach event handlers to all menu items
        int i = 0;
        for (MenuItem item : recentItems) {
            String fileName = RecentProjects.getItemsFromRecentsFile().get(i);
            Tooltip tooltip = new Tooltip(fileName);

            //add first few items to quick access buttons
            if (i < recentButtons.size()){
                Button recentButton = recentButtons.get(i);
                addItemToQuickAccess(fileName, recentButton);

                recentButton.setTooltip(tooltip);

                //note: this will still enable the button even if the project does not load properly
                recentButton.setDisable(false);
            }

            //add remaining items under the split menu button
            else{
                splitMenuButton.setDisable(false);
                splitMenuButton.getItems().add(item);
                item.setOnAction(event -> handleMenuItemSelection(item));
            }

            i++;
        }
    }

    private static void addItemToQuickAccess(String fileName, Button recentButton) {
        //set project file name
        File file = new File(fileName);


        recentButton.setText(file.getName());

        //set graphic to ? image if proper thumbnail cannot be found
        recentButton.setGraphic(new ImageView(new Image(new File("question-mark.png").toURI().toString())));
        recentButton.setContentDisplay(ContentDisplay.TOP);

        //get preview image from .k3d file or .ibr file

        //open file and convert to xml document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);

            //get view set path
            Element projectDomElement = (Element) document.getElementsByTagName("Project").item(0);
            Element viewSetDomElement = (Element) projectDomElement.getElementsByTagName("ViewSet").item(0);
            String viewSetPath = file.getParent() + "\\" + viewSetDomElement.getAttribute("src");

            //open images in view set path
            File viewSetFile = new File(viewSetPath);

            Scanner sc = new Scanner(viewSetFile);
            String imgsPath = null;
            String read;
            while (sc.hasNextLine()) {
                read = sc.nextLine();

                //if (read.equals("# Full resolution image file path")){
                if (read.equals("# Preview resolution image file path")) {
                    imgsPath = sc.nextLine();
                    //remove the first two chars of the path because it starts with "i "
                    imgsPath = imgsPath.substring(2);

                    //remove references to parent directories
                    String parentPrefix = "..\\";
                    while (imgsPath.startsWith(parentPrefix)) {
                        imgsPath = imgsPath.substring(parentPrefix.length());
                    }
                    break;
                }
            }

            if (imgsPath == null) {
                log.warn("Could not find preview image for " + file.getName());
                return;
            }

            String basePath = System.getProperty("user.home");
            File baseDir = new File(basePath);

            //build path off of home directory, otherwise correct path would not be found
            File imgFolder = new File(baseDir, imgsPath);

            String canonicalPath = imgFolder.getCanonicalPath();
            File resolvedFile = new File(canonicalPath);

            // Check if the path is a directory
            if (!resolvedFile.isDirectory()) {
                log.warn("Could not find preview image for " + file.getName());
                return;
            }

            // List child files
            String[] childFilePaths = resolvedFile.list();

            if (childFilePaths == null || childFilePaths.length == 0) {
                log.warn("Could not find preview image for " + file.getName());
                return;
            }

            String previewImgPath = canonicalPath + "\\" + childFilePaths[0];
            ImageView previewImgView = new ImageView(new Image(new File(previewImgPath).toURI().toString()));
            previewImgView.setFitHeight(80);
            previewImgView.setPreserveRatio(true);
            recentButton.setGraphic(previewImgView);
        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            log.warn("Could not find preview image for " + file.getName(), e);
        }
    }

    private static void handleMenuItemSelection(MenuItem item) {
        int i = 0;
        ArrayList<String> recentFileNames = (ArrayList<String>) RecentProjects.getItemsFromRecentsFile();
        //check recent projects menu for match
        for (MenuItem menuItem : MenubarController.getInstance().getRecentProjectsMenu().getItems()) {
            if (menuItem.equals(item)) {
                ProjectIO.getInstance().openProjectFromFile(new File(recentFileNames.get(i)));
                break;
            }
            ++i;
        }

        //check split menu button for match
        i = welcomeWindowController.recentButtons.size(); //need to offset the search by the number of buttons
        //ex. first split menu item is actually the sixth recent project if there are five buttons
        for (MenuItem menuItem : WelcomeWindowController.getInstance().recentProjectsSplitMenuButton.getItems()) {
            if (menuItem.equals(item)) {
                ProjectIO.getInstance().openProjectFromFile(new File(recentFileNames.get(i)));
                break;
            }
            ++i;
        }
    }

    public static void removeInvalidReferences() {
        ArrayList<String> recentItems = (ArrayList<String>) RecentProjects.getItemsFromRecentsFile();

        ArrayList<String> newRecentItems = new ArrayList<>();
        //iterate through list and remove items which do not exist in file system
        for (String item : recentItems){
            File file = new File(item);

            if (file.exists()){
                newRecentItems.add(item);
            }
        }

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
}
