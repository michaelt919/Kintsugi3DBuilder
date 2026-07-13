/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.core.JavaFXState;
import kintsugi3d.builder.javafx.core.RecentProjects;
import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;
import kintsugi3d.builder.javafx.util.SafeFloatStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CacheSettingsController implements SystemSettingsControllerBase
{
    @FXML private CheckBox sizeCheck;
    @FXML private CheckBox recentCheck;
    @FXML private CheckBox timeCheck;
    @FXML private TextField numGB;
    @FXML private TextField numRecent;
    @FXML private TextField numDays;
    @FXML private Label previewImageCacheLabel;
    @FXML private Label specularFitCacheLabel;
    @FXML private Label cacheSize;

    private static final Logger LOG = LoggerFactory.getLogger(CacheSettingsController.class);

    @Override
    public void initializeSettingsPage(Window parentWindow, JavaFXState state)
    {
        previewImageCacheLabel.setText(ApplicationFolders.getPreviewImagesRootDirectory().toString());
        specularFitCacheLabel.setText(ApplicationFolders.getFitCacheRootDirectory().toString());
        cacheSize.setText("Cache Size: " + String.format("%.2f", getCacheSize()) + "GB");

        bind(state.getSettingsModel());
    }

    public void bind(ObservableGeneralSettingsModel injectedSettingsModel)
    {
        sizeCheck.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("sizePromptEnabled"));
        recentCheck.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("recentPromptEnabled"));
        timeCheck.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("fileAgePromptEnabled"));

        numGB.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("cacheSizeLimit"),
            new SafeFloatStringConverter(32.0f));
        numRecent.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("recentProjectLimit"),
            new SafeNumberStringConverter(5));
        numDays.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("fileAgeLimit"),
            new SafeNumberStringConverter(30));
    }

    @FXML private void openDirectory(MouseEvent e){
        if (!(e.getSource() instanceof Label)) {
            return;
        }

        Label label = (Label) e.getSource();
        File file = new File(label.getText());
        if (!file.exists()){
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.NONE, "Cache path not found: " + label.getText(), ok);

            alert.setTitle("Cache path not found");
            alert.show();
            return;
        }

        try{
            Desktop.getDesktop().open(file);
        }
        catch(IOException ioe){
            ExceptionHandling.error("Failed to open project directory", ioe);
        }
    }

    @FXML private void clearCache()
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Cache");
        confirm.setHeaderText("Confirm cache clear?");
        confirm.setContentText("This will permanently remove all files in " + previewCacheDir + " and " + fitCacheDir
            + " and cannot be undone.  Are you sure?");

        confirm.showAndWait().ifPresent(response ->
        {
            if (response.equals(ButtonType.OK))
            {
                clearPreviewCache(previewCacheDir);
                clearFitCache(fitCacheDir);
            }
        });
    }

    private static void clearPreviewCache(File directory)
    {
        assert directory.isDirectory();
        File[] projects = directory.listFiles();
        assert projects != null;
        deletePreviewCacheFiles(directory, projects);
    }

    private static void clearFitCache(File directory)
    {
        assert directory.isDirectory();
        File[] projects = directory.listFiles();
        assert projects != null;
        deleteFitCacheFiles(directory, projects);
    }

    private static void deletePreviewCacheFiles(File directory, File[] projects)
    {
        for (File project : projects)
        {
            assert project.isDirectory();
            File[] resolutions = project.listFiles();
            assert resolutions != null;

            for (File resolution : resolutions)
            {
                assert resolution.isDirectory();
                File[] images = resolution.listFiles();
                assert images != null;

                for (File image : images)
                {
                    // Extra check due to danger of this operation
                    String imgName = image.toString();
                    assert imgName.startsWith(directory.toString());
                    assert imgName.toLowerCase(Locale.ROOT).endsWith(".png");
                    image.delete();
                }

                resolution.delete(); // Will only work if directory is empty.
            }

            project.delete(); // Will only work if directory is empty.
        }
    }

    private static void deleteFitCacheFiles(File directory, File[] projects)
    {
        for (File project : projects)
        {
            assert project.isDirectory();
            File[] resolutions = project.listFiles();
            assert resolutions != null;

            for (File resolution : resolutions)
            {
                assert resolution.isDirectory();

                // debug.png
                File debugImg = new File(resolution, "debug.png");
                assert debugImg.toString().startsWith(directory.toString());
                debugImg.delete();

                // sampleLocations.txt
                File sampleLocations = new File(resolution, "sampleLocations.txt");
                assert sampleLocations.toString().startsWith(directory.toString());
                sampleLocations.delete();

                // Everything left should be chunks folders (including the sampled folder)
                File[] chunks = resolution.listFiles();
                assert chunks != null;

                for (File chunk : chunks)
                {
                    assert chunk.isDirectory();
                    File[] images = chunk.listFiles();
                    assert images != null;

                    for (File image : images)
                    {
                        // Extra check due to danger of this operation
                        String imgName = image.toString();
                        assert imgName.startsWith(directory.toString());
                        assert imgName.toLowerCase(Locale.ROOT).endsWith(".png");
                        image.delete();
                    }

                    chunk.delete();
                }

                resolution.delete(); // Will only work if directory is empty.
            }

            project.delete(); // Will only work if directory is empty.
        }
    }

    @FXML private void cleanUpCacheButton()
    {
        cleanUpCache();
    }

    // need to have it work with settings
    public static void cleanUpCache()
    {
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        if (settingsModel.getBoolean("sizePromptEnabled"))
        {
            // TODO: add new deletion system for size
        }
        if (settingsModel.getBoolean("recentPromptEnabled"))
        {
            cleanUpNonRecentCache(settingsModel.getInt("recentProjectLimit"));
        }
        if (settingsModel.getBoolean("fileAgePromptEnabled"))
        {
            cleanOldCacheFiles(settingsModel.getInt("fileAgeLimit"));
        }
    }

    private static void cleanUpNonRecentCache(int numProjectsToKeep)
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Old Cache Files");
        confirm.setHeaderText("Confirm cache clean up?");
        confirm.setContentText("This will permanently remove all old files in " + previewCacheDir + " and " + fitCacheDir
            + " and cannot be undone.  Are you sure?");

        confirm.showAndWait().ifPresent(response ->
        {
            if (response.equals(ButtonType.OK))
            {
                clearNonRecentPreviewCache(previewCacheDir, numProjectsToKeep);
                clearNonRecentFitCache(fitCacheDir, numProjectsToKeep);
            }
        });
    }

    private static void clearNonRecentPreviewCache(File directory, int numProjectsToKeep)
    {
        assert directory.isDirectory();
//        File[] projects = directory.listFiles();
//        assert projects != null;
        // Select only cache directories that are not in the recently opened projects welcome dialogue.
        List<File> oldProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
        List<UUID> recentUUIDs = getRecentUUIDs(numProjectsToKeep);
        for (UUID recentUUID : recentUUIDs)
        {
            String cachePathFromUUID = directory + File.separator + recentUUID.toString();
            // Traverse backwards to not skip indices from removal
            for (int i = oldProjects.size() - 1; i >= 0; i--)
            {
                if (oldProjects.get(i).toString().equals(cachePathFromUUID))
                {
                    oldProjects.remove(i);
                }
            }
        }
        // Perform cache deletion on directories still in oldProjects.
        File[] oldProjectsArr = new File[oldProjects.size()];
        oldProjectsArr = oldProjects.toArray(oldProjectsArr);
        deletePreviewCacheFiles(directory, oldProjectsArr);
    }

    private static void clearNonRecentFitCache(File directory, int numProjectsToKeep)
    {
        assert directory.isDirectory();
        List<File> oldProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
        List<UUID> recentUUIDs = getRecentUUIDs(numProjectsToKeep);
        for (UUID recentUUID : recentUUIDs)
        {
            String cachePathFromUUID = directory + File.separator + recentUUID.toString();
            // Traverse backwards to not skip indices from removal
            for (int i = oldProjects.size() - 1; i >= 0; i--)
            {
                if (oldProjects.get(i).toString().equals(cachePathFromUUID))
                {
                    oldProjects.remove(i);
                }
            }
        }
        // Perform cache deletion on directories still in oldProjects.
        File[] oldProjectsArr = new File[oldProjects.size()];
        oldProjectsArr = oldProjects.toArray(oldProjectsArr);
        deleteFitCacheFiles(directory, oldProjectsArr);
    }

    private static List<UUID> getRecentUUIDs(int numProjectsToKeep)
    {
        List<String> recentProjects = RecentProjects.getItemsFromRecentsFile().stream().limit(numProjectsToKeep).collect(Collectors.toList());
        // Load view sets of recent projects to get their UUID.
        List<UUID> recentUUIDs = new ArrayList<>(recentProjects.size());
        for (String recentProject : recentProjects)
        {
            try
            {
                String projectName = recentProject.substring(recentProject.lastIndexOf(File.separator) + 1);
                ViewSet viewSet = ViewSetReaderFromVSET.getInstance()
                    .readFromFile(new File(recentProject + ".files" + File.separator + projectName + ".vset")).finish();
                recentUUIDs.add(viewSet.getUUID());
            }
            catch (IOException e)
            {
                ExceptionHandling.error("Failed to open project directory", e);
            }

        }
        return recentUUIDs;
    }

    private static void cleanOldCacheFiles(int dayLimit)
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Old Cache Files");
        confirm.setHeaderText("Confirm cache clean up?");
        confirm.setContentText("This will permanently remove all old files in " + previewCacheDir + " and " + fitCacheDir
            + " and cannot be undone.  Are you sure?");

        confirm.showAndWait().ifPresent(response ->
        {
            if (response.equals(ButtonType.OK))
            {
                clearOldFitCache(fitCacheDir, dayLimit);
                clearOldPreviewCache(previewCacheDir, dayLimit);
            }
        });
    }

    private static void clearOldPreviewCache(File directory, int dayLimit)
    {
        assert directory.isDirectory();
        List<File> oldProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
        filterOldCacheFiles(oldProjects, dayLimit);
        // Perform cache deletion on directories still in oldProjects.
        File[] oldProjectsArr = new File[oldProjects.size()];
        oldProjectsArr = oldProjects.toArray(oldProjectsArr);
        deletePreviewCacheFiles(directory, oldProjectsArr);
    }

    private static void clearOldFitCache(File directory, int dayLimit)
    {
        assert directory.isDirectory();
        List<File> oldProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
        filterOldCacheFiles(oldProjects, dayLimit);
        // Perform cache deletion on directories still in oldProjects.
        File[] oldProjectsArr = new File[oldProjects.size()];
        oldProjectsArr = oldProjects.toArray(oldProjectsArr);
        deleteFitCacheFiles(directory, oldProjectsArr);
    }

    private static void filterOldCacheFiles(List<File> projects, int dayLimit)
    {
        for (File dir : projects)
        {
            try
            {
                Instant lastAccess = Files.getLastModifiedTime(dir.toPath()).toInstant();
                Instant limit =  LocalDateTime.now().minusDays(dayLimit).atZone(ZoneId.systemDefault()).toInstant();
                if (lastAccess.isAfter(limit))
                {
                    projects.remove(dir);
                }
            }
            catch (IOException | RuntimeException e)
            {
                LOG.error("Error while finding cache file for cleanup", e);
            }
        }
    }

    private static long getDirectorySize(File directory)
    {
        long length = 0;
        File[] files = directory.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isFile())
                {
                    length += file.length();
                }
                else
                {
                    length += getDirectorySize(file);
                }
            }
        }
        return length;
    }

    public static double getCacheSize()
    {
        long fitSize = getDirectorySize(ApplicationFolders.getFitCacheRootDirectory().toFile());
        long previewSize = getDirectorySize(ApplicationFolders.getPreviewImagesRootDirectory().toFile());
        return (double) (fitSize + previewSize) / (1024 * 1024 * 1024); // Size returned in GB.
    }

    private static int getNumCachedProjects()
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();
        int previewSize = Objects.requireNonNull(previewCacheDir.listFiles()).length;
        int fitSize = Objects.requireNonNull(fitCacheDir.listFiles()).length;
        return Math.max(previewSize, fitSize);
    }

    private static boolean checkOldFilesExist()
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();
        List<File> cacheFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(previewCacheDir.listFiles())));
        cacheFiles.addAll(Arrays.asList(Objects.requireNonNull(fitCacheDir.listFiles())));

        Instant limit = LocalDateTime.now().minusDays(Global.state().getSettingsModel().getInt("fileAgeLimit"))
            .atZone(ZoneId.systemDefault()).toInstant();
        for (File dir : cacheFiles)
        {
            try
            {
                Instant lastAccess = Files.getLastModifiedTime(dir.toPath()).toInstant();
                if (lastAccess.isBefore(limit))
                {
                    return true;
                }
            }
            catch (IOException | RuntimeException e)
            {
                LOG.error("Error while finding cache file for cleanup", e);
            }
        }
        return false;
    }

    /**
     * Check if any cache cleanup prompts are selected and return true if any of their conditions are met.
     * @return
     */
    public static boolean checkForPrompt()
    {
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        if (settingsModel.getBoolean("sizePromptEnabled"))
        {
            if (getCacheSize() > settingsModel.getFloat("cacheSizeLimit"))
            {
                return true;
            }
        }
        if (settingsModel.getBoolean("recentPromptEnabled"))
        {
            if (getNumCachedProjects() > settingsModel.getInt("recentProjectLimit"))
            {
                return true;
            }
        }
        if (settingsModel.getBoolean("fileAgePromptEnabled"))
        {
            return checkOldFilesExist();
        }
        return false;
    }

//    // Not using this since it scares me.
//    private void deleteRecursively(File file)
//    {
//        deleteRecursively(file, file);
//    }
//
//    private void deleteRecursively(File original, File current)
//    {
//        if (current.isDirectory())
//        {
//            File[] contents = current.listFiles();
//            if (contents != null)
//            {
//                for (File f : contents)
//                {
//                    deleteRecursively(original, f);
//                }
//            }
//
//            // Extra check due to danger of this operation
//            assert current.toString().startsWith(original.toString());
//            current.delete();
//        }
//        else
//        {
//            // Extra check due to danger of this operation
//            assert current.toString().startsWith(original.toString());
//            current.delete();
//        }
//    }
}
