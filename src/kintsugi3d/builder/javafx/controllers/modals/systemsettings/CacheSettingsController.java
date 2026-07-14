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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
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
import java.nio.file.NotDirectoryException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
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
    @FXML private Label cacheSizeLabel;
    @FXML private Button cleanCacheButton;

    private static final Logger LOG = LoggerFactory.getLogger(CacheSettingsController.class);

    /**
     * This should ONLY be modified in one place, via the requestCacheSizeRefresh method.
     * Otherwise there would be bad race conditions.
     * TODO put this in an "observable cache model" once it exists?
     */
    private static final DoubleProperty cacheSizeGB = new SimpleDoubleProperty(-1); // -1 signifies uninitialized.

    /**
     * This should ONLY be modified in one place, via the requestCacheSizeRefresh method.
     * Otherwise there would be bad race conditions.
     * The intention is for this to just be used for UI display purposes, not internal thread synchronization logic.
     * TODO put this in an "observable cache model" once it exists?
     */
    private static final BooleanProperty cacheSizeCalcInProgress = new SimpleBooleanProperty(false);

    /**
     * Use a separate property for handling one-shot listeners
     * that we can dispose to dump in case the listener doesn't need to fire.
     * Important: should only be accessed in blocks synchronized on CACHE_SIZE_CALC_THREAD_LOCK
     * to prevent concurrent modification issues.
     * TODO put this in an "observable cache model" once it exists?
     */
    private static final List<ChangeListener<Number>> pendingCacheSizeCallbacks = new ArrayList<>(1);

    /**
     * Lock for multithreaded cache size calculation.
     * TODO put this in an "observable cache model" once it exists?
     */
    private static final Object CACHE_SIZE_CALC_THREAD_LOCK = new Object();

    /**
     * This thread will be set to a non-null value while running, and null otherwise
     * to prevent multiple thread running simultaneously.
     * TODO put this in an "observable cache model" once it exists?
     */
    private static volatile Thread cacheSizeCalcThread = null;

    @Override
    public void initializePage(Window parentWindow, JavaFXState state)
    {
        previewImageCacheLabel.setText(ApplicationFolders.getPreviewImagesRootDirectory().toString());
        specularFitCacheLabel.setText(ApplicationFolders.getFitCacheRootDirectory().toString());

        StringBinding cacheSizeTextBase = Bindings.createStringBinding(
            () -> String.format("Cache Size: %.2fGB", cacheSizeGB.get()), cacheSizeGB);

        // Three cases for cache size label:
        // 1. Cache size previously calculated
        // 2. Cache size previously calculated but being recalculated
        // 3. Cache size not yet calculated; calculation should be in progress.
        cacheSizeLabel.textProperty().bind(Bindings.when(cacheSizeGB.greaterThanOrEqualTo(0.0))
            .then(Bindings.when(cacheSizeCalcInProgress)
                .then(cacheSizeTextBase.concat(" (calculating...)"))
                .otherwise(cacheSizeTextBase))
            .otherwise(new ReadOnlyStringWrapper("Cache Size: (calculating...)")));

        // Request a refresh of the cache size without an explicit callback.
        requestCacheSizeRefresh();

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
            ButtonType ok = new ButtonType("OK", ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(AlertType.NONE, String.format("Cache path not found: %s", label.getText()), ok);

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
        confirm.setContentText(String.format("This will permanently remove all files in %s and %s and cannot be undone.  Are you sure?",
            previewCacheDir, fitCacheDir));

        confirm.showAndWait().ifPresent(response ->
        {
            if (response.equals(ButtonType.OK))
            {
                try
                {
                    clearPreviewCache(previewCacheDir);
                    clearFitCache(fitCacheDir);
                    requestCacheSizeRefresh();
                }
                catch(IOException e)
                {
                    handleCacheCleanupError(e);
                }
            }
        });
    }

    private static void handleCacheCleanupError(IOException e)
    {
        LOG.error(e.toString());
        ExceptionHandling.error("An error occurred while cleaning up cache.  Consider deleting cache files manually.", e);
    }

    private static void clearPreviewCache(File directory) throws IOException
    {
        if (!directory.isDirectory())
        {
            throw new NotDirectoryException(String.format("Invalid directory: %s", directory.getAbsolutePath()));
        }
        File[] projects = directory.listFiles();
        if (projects == null)
        {
            throw new NotDirectoryException(String.format("Invalid directory: %s", directory.getAbsolutePath()));
        }
        deletePreviewCacheFiles(directory, projects);
    }

    private static void clearFitCache(File directory) throws IOException
    {
        if (!directory.isDirectory())
        {
            throw new NotDirectoryException(String.format("Invalid directory: %s", directory.getAbsolutePath()));
        }
        File[] projects = directory.listFiles();
        if (projects == null)
        {
            throw new NotDirectoryException(String.format("Invalid directory: %s", directory.getAbsolutePath()));
        }
        deleteFitCacheFiles(directory, projects);
    }

    private static void deletePreviewCacheFiles(File directory, File[] projects) throws IOException
    {
        for (File project : projects)
        {
            LOG.info("Deleting preview cache for {}", project);

            if (!project.isDirectory())
            {
                throw new NotDirectoryException(String.format("Invalid directory: %s", project.getAbsolutePath()));
            }
            File[] resolutions = project.listFiles();
            if (resolutions == null)
            {
                throw new NotDirectoryException(String.format("Invalid directory: %s", project.getAbsolutePath()));
            }

            for (File resolution : resolutions)
            {
                if (!resolution.isDirectory())
                {
                    throw new NotDirectoryException(String.format("Invalid directory: %s", resolution.getAbsolutePath()));
                }
                File[] images = resolution.listFiles();
                if (images == null)
                {

                    throw new NotDirectoryException(String.format("Invalid directory: %s", resolution.getAbsolutePath()));
                }

                for (File image : images)
                {
                    // Extra check due to danger of this operation
                    String imgName = image.toString();
                    if (!imgName.startsWith(directory.toString()))
                    {
                        throw new IOException(String.format("Invalid image: %s", image.getAbsolutePath()));
                    }
                    if (!imgName.toLowerCase(Locale.ROOT).endsWith(".png"))
                    {
                        throw new IOException(String.format("Invalid image format: %s", image.getAbsolutePath()));
                    }
                    if (!image.delete())
                    {
                        throw new IOException(String.format("Image couldn't be deleted: %s", image.getAbsolutePath()));
                    }
                }

                if (!resolution.delete()) // Will only work if directory is empty.
                {
                    throw new IOException(String.format("Directory couldn't be deleted: %s", resolution.getAbsolutePath()));
                }
            }

            if (!project.delete()) // Will only work if directory is empty.
            {
                throw new IOException(String.format("Directory couldn't be deleted: %s", project.getAbsolutePath()));
            }
        }
    }

    private static void deleteFitCacheFiles(File directory, File[] projects) throws IOException
    {
        for (File project : projects)
        {
            LOG.info("Deleting fit cache for {}", project);

            if (!project.isDirectory())
            {
                throw new NotDirectoryException(String.format("Invalid directory: %s", project.getAbsolutePath()));
            }
            File[] resolutions = project.listFiles();
            if (resolutions == null)
            {
                throw new NotDirectoryException(String.format("Invalid directory: %s", project.getAbsolutePath()));
            }

            for (File resolution : resolutions)
            {
                if (!resolution.isDirectory())
                {
                    throw new NotDirectoryException(String.format("Invalid directory: %s", resolution.getAbsolutePath()));
                }

                // debug.png
                File debugImg = new File(resolution, "debug.png");
                if (!debugImg.toString().startsWith(directory.toString()))
                {
                    throw new IOException(String.format("Invalid debug image: %s", debugImg.getAbsolutePath()));
                }
                if (!debugImg.delete())
                {
                    LOG.info("debug.png not found at {}", debugImg.getAbsolutePath());
                }

                // sampleLocations.txt
                File sampleLocations = new File(resolution, "sampleLocations.txt");
                if (!sampleLocations.toString().startsWith(directory.toString()))
                {
                    throw new IOException(String.format("Invalid sample locations: %s", sampleLocations.getAbsolutePath()));
                }
                if (!sampleLocations.delete())
                {
                    throw new IOException(String.format("File couldn't be deleted: %s", sampleLocations.getAbsolutePath()));
                }

                // Everything left should be chunks folders (including the sampled folder)
                File[] chunks = resolution.listFiles();
                if  (chunks == null)
                {
                    throw new NotDirectoryException(String.format("Invalid directory: %s", resolution.getAbsolutePath()));
                }

                for (File chunk : chunks)
                {
                    if (!chunk.isDirectory())
                    {
                        throw new NotDirectoryException(String.format("Invalid chunk: %s", chunk.getAbsolutePath()));
                    }
                    File[] images = chunk.listFiles();
                    if (images == null)
                    {
                        throw new  NotDirectoryException(String.format("Invalid chunk: %s", chunk.getAbsolutePath()));
                    }

                    for (File image : images)
                    {
                        // Extra check due to danger of this operation
                        String imgName = image.toString();
                        if (!imgName.startsWith(directory.toString()))
                        {
                            throw new IOException(String.format("Invalid image: %s", image.getAbsolutePath()));
                        }
                        if (!imgName.toLowerCase(Locale.ROOT).endsWith(".png"))
                        {
                            throw new IOException(String.format("Invalid image format: %s", image.getAbsolutePath()));
                        }
                        if (!image.delete())
                        {
                            throw new IOException(String.format("Image couldn't be deleted: %s", image.getAbsolutePath()));
                        }
                    }

                    if (!chunk.delete())
                    {
                        throw new IOException(String.format("Directory couldn't be deleted: %s", chunk.getAbsolutePath()));
                    }
                }

                if (!resolution.delete()) // Will only work if directory is empty.
                {
                    throw new IOException(String.format("Directory couldn't be deleted: %s", resolution.getAbsolutePath()));
                }
            }

            if (!project.delete()) // Will only work if directory is empty.
            {
                throw new IOException(String.format("Directory couldn't be deleted: %s", project.getAbsolutePath()));
            }
        }
    }

    @FXML private void cleanUpCacheButton()
    {
        cleanUpCache();
    }

    public static void cleanUpCache()
    {
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        if (settingsModel.getBoolean("recentPromptEnabled") || settingsModel.getBoolean("fileAgePromptEnabled")
            || settingsModel.getBoolean("sizePromptEnabled"))
        {
            cleanUpBothCaches();
        }
    }

    private static void cleanUpBothCaches()
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Old Cache Files");
        confirm.setHeaderText("Confirm cache clean up?");
        confirm.setContentText(String.format("This will permanently remove files in %s and %s and cannot be undone.  Are you sure?",
            previewCacheDir, fitCacheDir));

        confirm.showAndWait().ifPresent(response ->
        {
            if (response.equals(ButtonType.OK))
            {
                new Thread(() ->
                {
                    try
                    {
                        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
                        int numProjectsToKeep = settingsModel.getInt("recentProjectLimit");
                        int dayLimit = settingsModel.getInt("fileAgeLimit");
                        cleanCacheSubDir(previewCacheDir, numProjectsToKeep, dayLimit);
                        cleanCacheSubDir(fitCacheDir, numProjectsToKeep, dayLimit);
                        requestCacheSizeRefresh();
                    }
                    catch (IOException e)
                    {
                        handleCacheCleanupError(e);
                    }
                }).start();
            }
        });
    }

    public static void cleanCacheSubDir(File directory, int numProjectsToKeep, int dayLimit) throws IOException
    {
        if (!directory.isDirectory())
        {
            throw new NotDirectoryException(String.format("Invalid directory: %s", directory));
        }
        // Select only cache directories that are not in the recently opened projects welcome dialogue.
        Set<File> deletableProjects = new HashSet<>(0);
        List<File> nonRecentProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
        List<File> oldProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        if (settingsModel.getBoolean("recentPromptEnabled"))
        {
            filterByRecentProjectLimit(directory, nonRecentProjects, numProjectsToKeep);
            deletableProjects.addAll(nonRecentProjects);
        }
        if (settingsModel.getBoolean("fileAgePromptEnabled")
            || (settingsModel.getBoolean("sizePromptEnabled") && (getCacheSizeGB() > settingsModel.getFloat("cacheSizeLimit"))))
        {
            filterByFileAgeLimit(oldProjects, dayLimit);
            deletableProjects.addAll(oldProjects);
        }
        // Perform cache deletion on directories still in oldProjects.
        File[] deletableProjectsArr = new File[deletableProjects.size()];
        deletableProjectsArr = deletableProjects.toArray(deletableProjectsArr);
        if ("preview".equals(directory.getName()))
        {
            deletePreviewCacheFiles(directory, deletableProjectsArr);
        }
        else if ("fit".equals(directory.getName()))
        {
            deleteFitCacheFiles(directory, deletableProjectsArr);
        }
    }

    private static void filterByRecentProjectLimit(File directory, List<File> oldProjects, int numProjectsToKeep)
    {
        List<UUID> recentUUIDs = getRecentUUIDs(numProjectsToKeep);
        for (UUID recentUUID : recentUUIDs)
        {
            String cachePathFromUUID = String.format("%s%s%s", directory, File.separator, recentUUID.toString());
            // Traverse backwards to not skip indices from removal
            for (int i = oldProjects.size() - 1; i >= 0; i--)
            {
                if (oldProjects.get(i).toString().equals(cachePathFromUUID))
                {
                    oldProjects.remove(i);
                }
            }
        }
    }

    private static void filterByFileAgeLimit(List<File> projects, int dayLimit)
    {
        for (int i = projects.size() - 1; i >= 0; i--)
        {
            try
            {
                Instant lastModified = Files.getLastModifiedTime(projects.get(i).toPath()).toInstant();
                Instant limit =  LocalDateTime.now().minusDays(dayLimit).atZone(ZoneId.systemDefault()).toInstant();
                if (lastModified.isAfter(limit))
                {
                    projects.remove(i);
                }
            }
            catch (IOException | RuntimeException e)
            {
                LOG.error("Error while finding cache file for cleanup", e);
            }
        }
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

    private static long getDirectorySize(File directory)
    {
        if (!Objects.equals(Thread.currentThread(), cacheSizeCalcThread))
        {
            throw new IllegalStateException("Thread is no longer the current cache size thread; terminating.");
        }

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
        LOG.debug("Directory size for {}: {}", directory, length);
        return length;
    }

    public static double getCacheSizeGB()
    {
        return cacheSizeGB.get();
    }

    /**
     * Requests that the cache size be recalculated without any callback.
     * If a request is already running when this method is invoked, another request will not be started.
     */
    public static void requestCacheSizeRefresh()
    {
        requestCacheSizeRefresh(value -> {});
    }

    /**
     * Requests that the cache size be recalculated.
     * If (and only if) the cache size is updated to a different value
     * (from this invocation or a parallel running invocation of the same method),
     * then the specified callback will be invoked once and only once with the updated value.
     * If a request is already running when this method is invoked,
     * another request will not be started but the additional callback will instead be attached to the
     * already-running request and invoked if that request yields a different value than the current one.
     * If invoked, the callback will always be invoked from the JavaFX Application Thread.
     * @param cacheSizeGBCallback
     */
    public static void requestCacheSizeRefresh(DoubleConsumer cacheSizeGBCallback)
    {
        //noinspection SynchronizationOnStaticField
        synchronized (CACHE_SIZE_CALC_THREAD_LOCK)
        {
            // Add one-shot listener with the specified callback before checking whether calculation is in progress.
            // Because cacheSizeGB should only be modified in one place (within a Platform.runLater fired by the worker thread),
            // synchronization will ensure that any update will not be queued (and thus not fired)
            // until after we exit this synchronized block.
            // This works whether we need to start the thread or one is already running.
            ChangeListener<Number> changeListener = new ChangeListener<>()
            {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
                {
                    cacheSizeGB.removeListener(this); // Remove first in case the callback throws an exception.
                    pendingCacheSizeCallbacks.remove(this);

                    try
                    {
                        cacheSizeGBCallback.accept(newValue.doubleValue());
                    }
                    catch (RuntimeException e)
                    {
                        LOG.error("Exception thrown by cache size callback", e);
                    }
                }
            };
            pendingCacheSizeCallbacks.add(changeListener); // add to our list first to avoid race conditions with JavaFX Application Thread
            cacheSizeGB.addListener(changeListener);

            if (cacheSizeCalcThread == null) // only want one thread running at a time.
            {
                cacheSizeCalcInProgress.set(true);
                cacheSizeCalcThread = new Thread(() ->
                {
                    try
                    {
                        // Refresh the cache size.  This takes a while.
                        double newCacheSizeGB = calcCacheSize();

                        Platform.runLater(() ->
                        {
                            // prevent race condition if another call to requestCacheSizeRefresh comes in
                            // while updating and cleaning up listeners.
                            //noinspection SynchronizationOnStaticField
                            synchronized (CACHE_SIZE_CALC_THREAD_LOCK)
                            {
                                try
                                {
                                    // This will trigger the listener created above.
                                    cacheSizeGB.set(newCacheSizeGB);
                                }
                                finally
                                {
                                    // Discard any listeners that didn't fire (i.e. if the value didn't change).
                                    pendingCacheSizeCallbacks.forEach(cacheSizeGB::removeListener);
                                    pendingCacheSizeCallbacks.clear();

                                    // This thread is now done.
                                    // Set the thread reference to null to indicate that future refresh requests
                                    // should actually start a new thread.
                                    // We actually wait until after the JavaFX update via Platform.runLater
                                    // so that another thread doesn't start before the update is fully processed.
                                    cacheSizeCalcThread = null;
                                    cacheSizeCalcInProgress.set(false);
                                }
                            }
                        });
                    }
                    catch (RuntimeException e)
                    {
                        LOG.error("Error calculating cache size", e);

                        //noinspection SynchronizationOnStaticField
                        synchronized (CACHE_SIZE_CALC_THREAD_LOCK)
                        {
                            // Discard any listeners that didn't fire since an exception was thrown.
                            pendingCacheSizeCallbacks.forEach(cacheSizeGB::removeListener);
                            pendingCacheSizeCallbacks.clear();

                            // If an exception occurs, we want to still note that the thread is done.
                            cacheSizeCalcThread = null;
                            cacheSizeCalcInProgress.set(false);
                        }
                    }
                }, "Cache Size Calculation");

                cacheSizeCalcThread.start();
            }

            // If cache size calc thread is already running, then it should eventually trigger the listener registered above.
        }
    }

    private static double calcCacheSize()
    {
        long fitSize = getDirectorySize(ApplicationFolders.getFitCacheRootDirectory().toFile());
        long previewSize = getDirectorySize(ApplicationFolders.getPreviewImagesRootDirectory().toFile());
        return (double) (fitSize + previewSize) / (1024 * 1024 * 1024); // Size in GB.
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
        Collection<File> cacheFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(previewCacheDir.listFiles())));
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
     * Check if any cache cleanup conditions are enabled and triggered.
     * If so, a callback will be fired with the current cache size in GB.
     * If no enabled cache cleanup conditions are met, this method does nothing, although it may still trigger
     * calculation of the cache size if said calculation has not yet been performed.
     * If the cache size needs to be calculated, the cleanup conditions will be
     * checked asynchronously in a worker thread that will also store the cache size.
     * Otherwise, the conditions will be checked immediately.
     * @param promptWithCacheSizeGB The callback, which takes as input the current cache size in GB.
     */
    public static void requestPromptForCacheCleanup(
        Consumer<Double> promptWithCacheSizeGB, Consumer<Double> noCleanupNeededWithCacheSizeGB)
    {
        if (cacheSizeGB.get() < 0.0)
        {
            // Cache size needs to be calculated.
            requestCacheSizeRefresh(newCacheSize ->
                checkforCleanupPrompts(newCacheSize, promptWithCacheSizeGB, noCleanupNeededWithCacheSizeGB));
        }
        else
        {
            // Cache size is already calculated, just show the prompt.
            checkforCleanupPrompts(cacheSizeGB.get(), promptWithCacheSizeGB, noCleanupNeededWithCacheSizeGB);
        }
    }

    private static void checkforCleanupPrompts(double newCacheSize, Consumer<Double> promptWithCacheSizeGB,
                                               Consumer<Double> noCleanupNeededWithCacheSizeGB)
    {
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        if (settingsModel.getBoolean("sizePromptEnabled"))
        {
            if (getCacheSizeGB() > settingsModel.getFloat("cacheSizeLimit"))
            {
                promptWithCacheSizeGB.accept(newCacheSize);
            }
        }
        if (settingsModel.getBoolean("recentPromptEnabled"))
        {
            if (getNumCachedProjects() > settingsModel.getInt("recentProjectLimit"))
            {
                promptWithCacheSizeGB.accept(newCacheSize);
            }
        }
        if (settingsModel.getBoolean("fileAgePromptEnabled"))
        {
            if (checkOldFilesExist())
            {
                promptWithCacheSizeGB.accept(newCacheSize);
            }
        }

        // If the cache does not need to be cleaned up, then fire the "no cleanup needed" callback.
        noCleanupNeededWithCacheSizeGB.accept(newCacheSize);
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