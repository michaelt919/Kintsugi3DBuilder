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

package kintsugi3d.builder.javafx.core;

import javafx.scene.control.*;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.app.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class RecentProjects
{
    public static final File RECENT_PROJECTS_FILE
        = new File(ApplicationFolders.getUserAppDirectory().toFile(), "recentFiles.txt");

    public static final int MAX_RECENT_PROJECTS = 20;

    private static final Logger LOG = LoggerFactory.getLogger(RecentProjects.class);
    private static File recentDirectory = null;

    private RecentProjects()
    {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> getItemsFromRecentsFile()
    {
        List<String> projectItems = List.of();

        if (RECENT_PROJECTS_FILE.exists())
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(RECENT_PROJECTS_FILE.getAbsolutePath(), StandardCharsets.UTF_8)))
            {
                projectItems = reader.lines().limit(MAX_RECENT_PROJECTS).collect(Collectors.toUnmodifiableList());
            }
            catch (IOException e)
            {
                LOG.error("Could not get items from recent files list", e);
            }
        }

        //remove duplicates while maintaining the same order (regular HashSet does not maintain order)
        return new ArrayList<>(new LinkedHashSet<>(projectItems));
    }

    public static List<MenuItem> getMenuItems()
    {
        List<String> items = getItemsFromRecentsFile();
        return getMenuItems(items);
    }

    public static List<MenuItem> getMenuItems(Collection<String> items)
    {
        List<MenuItem> customMenuItems = new ArrayList<>(items.size());
        int i = 0;

        //attach tooltips and event handlers
        for (String item : items)
        {
            String fileName = getItemsFromRecentsFile().get(i);
            String shortPath = shortenedPath(item);

            if (OperatingSystem.getCurrentOS() == OperatingSystem.MACOS)
            {
                // MacOS doesn't support custom menu items
                MenuItem menuItem = new MenuItem(shortPath);
                menuItem.setOnAction(event -> onMenuItemAction(fileName));
                customMenuItems.add(menuItem);
            }
            else
            {
                CustomMenuItem menuItem = new CustomMenuItem(new Label(shortPath));
                menuItem.setOnAction(event -> onMenuItemAction(fileName));

                Tooltip tooltip = new Tooltip(fileName);
                Tooltip.install(menuItem.getContent(), tooltip);

                customMenuItems.add(menuItem);
            }

            ++i;
        }

        return customMenuItems;
    }

    private static void onMenuItemAction(String fileName)
    {
        ProjectIO.getInstance().openProjectFromFileWithPrompt(new File(fileName));
    }

    public static String shortenedPath(String path)
    {
        File file = new File(path);
        File ancestorFile = getAncestorFile(file);

        return String.format("%s...%s%s", ancestorFile.getAbsolutePath(), File.separator, file.getName());
    }

    private static File getAncestorFile(File file)
    {
        File ancestorFile = file;
        while (ancestorFile.getParentFile() != null)
        {
            ancestorFile = ancestorFile.getParentFile();
        }
        return ancestorFile;
    }

    public static void addToRecentFiles(String fileName)
    {
        List<String> existingFileNames = getItemsFromRecentsFile();

        // Check if the fileName is already present
        existingFileNames.remove(fileName); // Remove it from its current position

        // Add the fileName to the front of the List
        existingFileNames.add(0, fileName);

        // Drop down to the max number of recent projects.
        existingFileNames = existingFileNames.subList(0, MAX_RECENT_PROJECTS);

        // Write the updated content back to the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(RECENT_PROJECTS_FILE, StandardCharsets.UTF_8)))
        {
            for (String name : existingFileNames)
            {
                writer.println(name);
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed to update recent files list", e);
        }

        //update list of recent projects in program
        updateAllControlStructures();
    }

    public static void updateAllControlStructures()
    {
        if (MainWindowController.getInstance() != null)
        {
            updateRecentProjectsInMenuBar();
        }

        if (WelcomeWindowController.getInstance() != null)
        {
            WelcomeWindowController.getInstance().updateRecentProjects();
        }
    }


    private static void updateRecentProjectsInMenuBar()
    {
        Menu recentProjsList = MainWindowController.getInstance().getRecentProjectsMenu();
        Menu cleanRecentProjectsMenu = MainWindowController.getInstance().getCleanRecentProjectsMenu();

        recentProjsList.getItems().clear();

        List<MenuItem> recentItems = getMenuItems();

        recentProjsList.getItems().addAll(recentItems);

        //disable menus if there are no recent projects, otherwise enable
        boolean isListEmpty = recentProjsList.getItems().isEmpty();
        recentProjsList.setDisable(isListEmpty);
        cleanRecentProjectsMenu.setDisable(isListEmpty);
    }

    public static void removeInvalidReferences()
    {
        List<String> newRecentItems = getItemsFromRecentsFile().stream()
            .map(File::new)
            .filter(File::exists)
            .map(File::getAbsolutePath)
            .collect(Collectors.toList());

        // Write the updated content back to the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(RECENT_PROJECTS_FILE, StandardCharsets.UTF_8)))
        {
            for (String name : newRecentItems)
            {
                writer.println(name);
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed to update recent files list while removing invalid references.", e);
        }

        updateAllControlStructures();
    }

    public static void removeAllReferences()
    {
        //wipe recent projects list
        try (FileWriter fileWriter = new FileWriter(RECENT_PROJECTS_FILE.getAbsolutePath(), StandardCharsets.UTF_8, false))
        {
            fileWriter.write("");
        }
        catch (IOException e)
        {
            LOG.error("Could not write to recent files list", e);
        }

        updateAllControlStructures();
    }

    public static String getMostRecentProjectPath()
    {
        return getItemsFromRecentsFile().get(0);
    }

    //use these functions to make file selection more user-friendly across multiple File/Directory Choosers
    public static void setMostRecentDirectory(File file)
    {
        recentDirectory = file;
    }

    public static File getMostRecentDirectory()
    {
        if ((recentDirectory != null) && recentDirectory.exists())
        {
            return recentDirectory;
        }

        //loop through recent files and assign/return the first existing one
        for (String path : getItemsFromRecentsFile())
        {
            File file = new File(path);
            if (file.exists())
            {
                recentDirectory = file.getParentFile();
                return recentDirectory;
            }
        }

        return new File(System.getProperty("user.home"));
    }
}
