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

import javafx.scene.control.MenuItem;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RecentProjects extends WelcomeWindowController {

    private static File recentProjectsFile = new File("src/main/resources/recentFiles.txt");//TODO: NEW LOCATION?
    private static WelcomeWindowController welcomeWindowController;
    private static MenubarController menubarController;

    public static List<String> getItemsFromRecentsFile() {
        List<String> projectItems = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(recentProjectsFile.getAbsolutePath()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String projectItem = line;
                projectItems.add(projectItem);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        try (BufferedReader reader = new BufferedReader(new FileReader(recentProjectsFile))) {
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
        try (PrintWriter writer = new PrintWriter(new FileWriter(recentProjectsFile))) {
            for (String name : existingFileNames) {
                writer.println(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //update list of recent projects in program
        welcomeWindowController.updateRecentProjectsButton();
        menubarController.updateRecentProjectsMenu();
    }

    public static void initializeWelcomeWindowController(WelcomeWindowController welcomeWindowController) {
        RecentProjects.welcomeWindowController = welcomeWindowController;
    }

    public static void initializeMenubarController(MenubarController menubarController) {
        RecentProjects.menubarController = menubarController;
    }
}
