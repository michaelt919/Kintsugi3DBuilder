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

import javafx.scene.control.*;
import kintsugi3d.builder.app.OperatingSystem;
import kintsugi3d.builder.core.RecentProjects;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RecentProjectsHelper
{
    private RecentProjectsHelper()
    {
    }

    public static List<MenuItem> getMenuItems(Collection<String> items)
    {
        List<MenuItem> customMenuItems = new ArrayList<>(items.size());
        int i = 0;

        //attach tooltips and event handlers
        for (String item : items)
        {
            String fileName = RecentProjects.getRecentProjectFilenames().get(i);
            String shortPath = RecentProjects.shortenedPath(item);

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

    private static void updateRecentProjectsInMenuBar()
    {
        Menu recentProjsList = MainWindowController.getInstance().getRecentProjectsMenu();
        Menu cleanRecentProjectsMenu = MainWindowController.getInstance().getCleanRecentProjectsMenu();

        recentProjsList.getItems().clear();

        List<String> items = RecentProjects.getRecentProjectFilenames();
        List<MenuItem> recentItems = getMenuItems(items);

        recentProjsList.getItems().addAll(recentItems);

        //disable menus if there are no recent projects, otherwise enable
        boolean isListEmpty = recentProjsList.getItems().isEmpty();
        recentProjsList.setDisable(isListEmpty);
        cleanRecentProjectsMenu.setDisable(isListEmpty);
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
}
