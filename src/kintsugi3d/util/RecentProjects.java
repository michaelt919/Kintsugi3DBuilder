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
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObject;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.gl.util.UnzipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
                e.printStackTrace();
            }
        }

        //remove duplicates while maintaining the same order (regular HashSet does not maintain order)
        return new ArrayList<>(new LinkedHashSet<>(projectItems));
    }

    public static List<MenuItem> getItemsAsMenuItems(){
        List<String> items = RecentProjects.getItemsFromRecentsFile();

        List<MenuItem> menuItems = new ArrayList<>();
        for (String item : items){
            menuItems.add(new MenuItem(item));
        }

        return menuItems;
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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        //update list of recent projects in program
        welcomeWindowController.updateRecentProjectsButton();
        MenubarController.getInstance().updateRecentProjectsMenu();
    }

    public static void initializeWelcomeWindowController(WelcomeWindowController welcomeWindowController) {
        RecentProjects.welcomeWindowController = welcomeWindowController;
    }

    public static void updateRecentProjectsControl(Object menu) {
        //change formatting in menu? Currently just the path of the object
        if (menu instanceof Menu){
            Menu castedMenu = (Menu) menu;
            updateRecentProjectsMenu(castedMenu);
        }

        else if (menu instanceof SplitMenuButton){
            SplitMenuButton castedSplitMenuButton = (SplitMenuButton) menu;
            updateRecentProjectsSplitMenuButton(castedSplitMenuButton);
        }
    }

    //looks like updateRecentProjectsMenu() and updateRecentProjectsSplitMenuButton should
    //be merged into one function because they have identical code, but no class encompasses them
    //both while maintaining menu functionality (getItems(), setDisable(), etc.)
    private static void updateRecentProjectsMenu(Menu menu){
        menu.getItems().clear();

        ArrayList<MenuItem> recentItems = (ArrayList<MenuItem>) RecentProjects.getItemsAsMenuItems();

        menu.getItems().addAll(recentItems);

        //disable button if there are no recent projects
        if (menu.getItems().isEmpty()) {
            menu.setDisable(true);
        }

        //attach event handlers to all menu items
        for (MenuItem item : recentItems) {
            item.setOnAction(event -> handleMenuItemSelection(item));
        }
    }

    private static void updateRecentProjectsSplitMenuButton(SplitMenuButton menu) {
        menu.getItems().clear();

        ArrayList<MenuItem> recentItems = (ArrayList<MenuItem>) RecentProjects.getItemsAsMenuItems();

        //attach event handlers to all menu items
        int i = -1;
        ArrayList<Button> recentButtons = welcomeWindowController.recentButtons;
        ArrayList<String> recentStrings = welcomeWindowController.recentButtonFiles;
        for (MenuItem item : recentItems) {
            i++; //increment i at the beginning of the loop instead of the end because "continue" and "break" are used

            //add first few items to quick access buttons
            if (i < recentButtons.size()){

                //set project file name
                String fileName = item.getText();
                File file = new File(fileName);
                recentButtons.get(i).setText(file.getName());
                recentStrings.add(fileName);

                //TODO: ADD IMAGE TO PROJECT BUTTON
                //set graphic to ? image if proper thumbnail cannot be found
                recentButtons.get(i).setGraphic(new ImageView(new Image(new File("question-mark.png").toURI().toString())));
                recentButtons.get(i).setContentDisplay(ContentDisplay.TOP);

                //get preview image from .k3d file or .ibr file
                if (file.getAbsolutePath().matches(".*" + "\\.k3d") ||
                        file.getAbsolutePath().matches(".*" + "\\.ibr")
                ){
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
                            continue;
                        }

                        String basePath = System.getProperty("user.home");
                        File baseDir = new File(basePath);

                        //build path off of home directory, otherwise correct path would not be found
                        File imgFolder = new File(baseDir, imgsPath);

                        String canonicalPath = imgFolder.getCanonicalPath();
                        File resolvedFile = new File(canonicalPath);

                        // Check if the path is a directory
                        if (!resolvedFile.isDirectory()) {
                            continue;
                        }

                        // List child files
                        String[] childFilePaths = resolvedFile.list();

                        if (childFilePaths == null || childFilePaths.length == 0) {
                            continue;
                        }

                        String previewImgPath = canonicalPath + "\\" + childFilePaths[0];
                        ImageView previewImgView = new ImageView(new Image(new File(previewImgPath).toURI().toString()));
                        previewImgView.setFitHeight(80);
                        previewImgView.setPreserveRatio(true);
                        recentButtons.get(i).setGraphic(previewImgView);



                    } catch (ParserConfigurationException | IOException | SAXException e) {
                        log.error("Could not find preview image for " + file.getName(), e);
                    }

                }

            }

            //add remaining items under the split menu button
            else{
                menu.getItems().addAll(item);
                item.setOnAction(event -> handleMenuItemSelection(item));
            }
        }

        //disable button if there are no recent projects
        if (menu.getItems().isEmpty()) {
            menu.setDisable(true);
        }
    }

    private static void handleMenuItemSelection(MenuItem item) {
        String projectName = item.getText();
        ProjectIO.getInstance().openProjectFromFile(new File(projectName));
    }
}
