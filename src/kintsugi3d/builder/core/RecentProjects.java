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

package kintsugi3d.builder.core;

import kintsugi3d.builder.app.ApplicationFolders;
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

    private static final Collection<Runnable> RECENT_FILES_CHANGED_LISTENERS = new ArrayList<>(1);

    private RecentProjects()
    {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> getRecentProjectFilenames()
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
        List<String> existingFileNames = getRecentProjectFilenames();

        // Check if the fileName is already present
        existingFileNames.remove(fileName); // Remove it from its current position

        // Add the fileName to the front of the List
        existingFileNames.add(0, fileName);

        // Drop down to the max number of recent projects.
        existingFileNames = existingFileNames.subList(0, Math.min(existingFileNames.size(), MAX_RECENT_PROJECTS));

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
        fireListeners();
    }

    public static void removeInvalidReferences()
    {
        List<String> newRecentItems = getRecentProjectFilenames().stream()
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

        fireListeners();
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

        fireListeners();
    }

    public static String getMostRecentProjectPath()
    {
        return getRecentProjectFilenames().get(0);
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
        for (String path : getRecentProjectFilenames())
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

    public static void addRecentProjectsChangedListener(Runnable listener)
    {
        RECENT_FILES_CHANGED_LISTENERS.add(listener);
    }

    private static void fireListeners()
    {
        for (Runnable listener : RECENT_FILES_CHANGED_LISTENERS)
        {
            listener.run();
        }
    }
}
