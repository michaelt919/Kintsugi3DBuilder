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

package kintsugi3d.builder.state;

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.RecentProjects;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;

public abstract class CacheModelBase implements CacheModel
{
    private static final Logger LOG = LoggerFactory.getLogger(CacheModelBase.class);

    private final AtomicBoolean cacheCleanupInProgress;

    /**
     * Lock for multithreaded cache size calculation.
     */
    private final Object cacheSizeCalcThreadLock;

    /**
     * This thread will be set to a non-null value while running, and null otherwise
     * to prevent multiple thread running simultaneously.
     */
    private volatile Thread cacheSizeCalcThread;

    protected CacheModelBase()
    {
        cacheCleanupInProgress = new AtomicBoolean(false);
        cacheSizeCalcThreadLock = new Object();
        cacheSizeCalcThread = null;
    }

    protected abstract void updateCacheSize(long newCacheSize, Map<String, Long> newProjectSizes, Runnable onCompleteCallback);

    protected abstract void setCacheSizeCalcInProgress(boolean cacheSizeCalcInProgress);

    protected abstract void addCacheSizeChangeCallback(DoubleConsumer cacheSizeChangeCallback);

    protected abstract void removeAllCacheSizeChangeCallbacks();

    protected Map<File, Consumer<File[]>> getDeleteMethods()
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();

        return Map.of(
            previewCacheDir, files -> tryDeletePreviewCacheFiles(previewCacheDir, files),
            fitCacheDir, files -> tryDeleteFitCacheFiles(fitCacheDir, files));
    }

    private Collection<File> getCleanableCacheDirectories()
    {
        return getDeleteMethods().keySet();
    }

    protected void handleCacheCleanupError(Exception e)
    {
        LOG.error(e.toString());
    }

    @Override
    public void requestClearCache()
    {
        Map<File, Consumer<File[]>> deleteMethods = getDeleteMethods();

        new Thread(() ->
        {
            try
            {
                if (cacheCleanupInProgress.compareAndSet(false, true))
                {
                    try
                    {
                        for (var entry : deleteMethods.entrySet())
                        {
                            clearCache(entry.getKey(), entry.getValue());
                        }
                    }
                    finally
                    {
                        cacheCleanupInProgress.set(false);
                    }

                    requestCacheSizeRefresh();
                }
            }
            catch (IOException e)
            {
                handleCacheCleanupError(e);
            }
        }, "Clear Cache").start();
    }

    private static void clearCache(File directory, Consumer<File[]> deleteMethod) throws IOException
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
        deleteMethod.accept(projects);
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
                        throw new IOException(String.format("Invalid image: %s.  Expected directory: %s",
                            imgName, directory));
                    }
                    if (!imgName.toLowerCase(Locale.ROOT).endsWith(".png"))
                    {
                        throw new IOException(String.format("Invalid image format: %s", imgName));
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

    private void tryDeletePreviewCacheFiles(File directory, File[] projects)
    {
        try
        {
            deletePreviewCacheFiles(directory, projects);
        }
        catch (IOException e)
        {
            handleCacheCleanupError(e);
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
                    throw new IOException(String.format("Invalid debug image: %s.  Expected directory: %s.",
                        debugImg, directory));
                }
                if (!debugImg.delete())
                {
                    LOG.info("debug.png not found at {}", debugImg.getAbsolutePath());
                }

                // sampleLocations.txt
                File sampleLocations = new File(resolution, "sampleLocations.txt");
                if (!sampleLocations.toString().startsWith(directory.toString()))
                {
                    throw new IOException(String.format("Invalid sample locations: %s.  Expected directory: %s.",
                        sampleLocations, directory));
                }
                if (!sampleLocations.delete())
                {
                    throw new IOException(String.format("File couldn't be deleted: %s", sampleLocations.getAbsolutePath()));
                }

                // Everything left should be chunks folders (including the sampled folder)
                File[] chunks = resolution.listFiles();
                if (chunks == null)
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
                        throw new NotDirectoryException(String.format("Invalid chunk: %s", chunk.getAbsolutePath()));
                    }

                    for (File image : images)
                    {
                        // Extra check due to danger of this operation
                        String imgName = image.toString();
                        if (!imgName.startsWith(directory.toString()))
                        {
                            throw new IOException(String.format("Invalid image: %s.  Expected directory: %s,",
                                imgName, directory));
                        }
                        if (!imgName.toLowerCase(Locale.ROOT).endsWith(".png"))
                        {
                            throw new IOException(String.format("Invalid image format: %s", imgName));
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

    private void tryDeleteFitCacheFiles(File directory, File[] projects)
    {
        try
        {
            deleteFitCacheFiles(directory, projects);
        }
        catch (IOException e)
        {
            handleCacheCleanupError(e);
        }
    }

    @Override
    public void requestCleanUpBothCaches(Map<File, Consumer<File[]>> deleteMethods)
    {
        new Thread(() ->
        {
            try
            {
                cleanCacheSubDirs(deleteMethods);
            }
            catch (IOException e)
            {
                handleCacheCleanupError(e);
            }
        }, "Clean Up Cache").start();
    }

    /**
     * Cleans up the cache based on user settings, and then refreshes the cache size.
     *
     * @param deleteMethods
     * @throws IOException
     */
    private void cleanCacheSubDirs(Map<File, Consumer<File[]>> deleteMethods) throws IOException
    {
        if (cacheCleanupInProgress.compareAndSet(false, true))
        {
            try
            {
                GeneralSettingsModel settingsModel = Global.state().getSettingsModel();

                for (var deleteMethod : deleteMethods.entrySet())
                {
                    File directory = deleteMethod.getKey();

                    if (!directory.isDirectory())
                    {
                        throw new NotDirectoryException(String.format("Invalid directory: %s", directory));
                    }
                    // Select only cache directories that are not in the recently opened projects welcome dialogue.
                    Collection<File> deletableProjects = new HashSet<>(0);
                    List<File> nonRecentProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
                    List<File> oldProjects = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));

                    if (settingsModel.getBoolean("recentPromptEnabled"))
                    {
                        filterByRecentProjectLimit(directory, nonRecentProjects);
                        deletableProjects.addAll(nonRecentProjects);
                    }

                    if (settingsModel.getBoolean("fileAgePromptEnabled"))
                    {
                        filterByFileAgeLimit(oldProjects);
                        deletableProjects.addAll(oldProjects);
                    }

                    // Perform cache deletion on directories still in oldProjects.
                    File[] deletableProjectsArr = deletableProjects.toArray(File[]::new);

                    deleteMethod.getValue().accept(deletableProjectsArr);
                }

                // Recalculate the size of the cache after cleaning up files by age or by presence in recent files list.
                // Even if we still need to remove more projects due to cache size limit, it is necessary to first refresh
                // the cache size because a user could, in theory, delete one of the more recent projects from their hard drive
                // and then purge the recent files list manually.  The non-recent cache cleanup will then delete that project's
                // cache, meaning that it may be possible to keep the cache for one or more older projects that have not been
                // deleted while keeping within the cache size limit.
                // On the other hand, if the cache size limit is not enabled, this will just be the final refresh.
                requestCacheSizeRefresh(null, () ->
                    // Callback will be on the JavaFX thread so we need to spawn a new thread
                    new Thread(() ->
                    {
                        try
                        {
                            if (settingsModel.getBoolean("sizePromptEnabled") && (getCacheSizeGB() > settingsModel.getFloat("cacheSizeLimit")))
                            {
                                // This method will keep deleting projects until we're within the cache size limit.
                                cleanUpBySizeLimit();

                                // Refresh the cache size again to display the final reduced size.
                                requestCacheSizeRefresh();
                            }
                        }
                        finally
                        {
                            cacheCleanupInProgress.set(false);
                        }
                    }, "Clean Up Cache By Size Limit").start());
            }
            catch (RuntimeException e)
            {
                cacheCleanupInProgress.set(false);
                throw e;
            }
        }
    }

    private static void filterByRecentProjectLimit(File directory, List<File> oldProjects)
    {
        List<UUID> recentUUIDs = getRecentUUIDs(Global.state().getSettingsModel().getInt("recentProjectLimit"));
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

    private static void filterByFileAgeLimit(List<File> projects)
    {
        for (int i = projects.size() - 1; i >= 0; i--)
        {
            try
            {
                Instant lastModified = Files.getLastModifiedTime(projects.get(i).toPath()).toInstant();
                Instant limit = LocalDateTime.now().minusDays(Global.state().getSettingsModel().getInt("fileAgeLimit"))
                    .atZone(ZoneId.systemDefault()).toInstant();
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

    private void cleanUpBySizeLimit()
    {
        Map<String, Collection<File>> cleanableCacheProjects = new HashMap<>(getNumCachedProjects());

        // Build a mapping from project IDs to all cache directories associated with that project.
        // This is necessary since a project might have one cache for a project but not another,
        // but all need to be cleaned up when cleaning down to a size limit.
        for (File directory : getDeleteMethods().keySet())
        {
            for (File projectCacheDir : Objects.requireNonNull(directory.listFiles()))
            {
                // The project cache directory name should be the same regardless of which cache (i.e. preview or fit) is being cleaned.
                String projectID = projectCacheDir.getName();

                // i.e. a list that will contain the fit and preview directories for a given project
                Collection<File> projectDirList = cleanableCacheProjects.computeIfAbsent(
                    // Create the ArrayList if not already in the map.
                    projectID, id -> new ArrayList<>(getCleanableCacheDirectories().size()));

                // Add the current directory to the list
                projectDirList.add(projectCacheDir);
            }
        }

        List<String> retainedProjectIDs =
            cleanableCacheProjects.entrySet().stream()
                .sorted(Comparator.comparingLong(
                        (Entry<String, Collection<File>> entry) -> // sort with least recently modified (i.e. "oldest") first
                            entry.getValue().stream()
                                .mapToLong(File::lastModified).max() // find most recent modification over various cache directories (i.e. preview, fit)
                                .orElse(0L)) // shouldn't really ever need the default
                    .reversed()) // Reverse so that the oldest projects are now last.
                .map(Entry::getKey) // Keep just the key / project ID
                .collect(Collectors.toCollection(ArrayList<String>::new)); // Explicit ArrayList constructor to ensure mutability

        long prevCacheSize;
        Map<String, Long> prevProjectSizes;

        // Prevent race condition, as the project size map and the total cache size could be updated
        // as a result of a concurrent cache size recalculation.
        // We just get a snapshot here and work off that snapshot.
        // It's possible that the contents on disk will no longer reflect this but our best bet is still to just
        // optimize with the information that we had when the process started.
        synchronized (cacheSizeCalcThreadLock)
        {
            prevCacheSize = getCacheSizeBytes();
            prevProjectSizes = getProjectSizes();
        }

        long freedSpace = 0;
        int oldestProjectIndex = retainedProjectIDs.size() - 1;

        while (oldestProjectIndex >= 0 &&
            (prevCacheSize - freedSpace) * BYTES_TO_GB > Global.state().getSettingsModel().getDouble("cacheSizeLimit"))
        {
            String oldestProjectID = retainedProjectIDs.get(oldestProjectIndex);
            Long projectSize = prevProjectSizes.get(oldestProjectID);

            if (projectSize != null)
            {
                freedSpace += projectSize;
                retainedProjectIDs.remove(oldestProjectIndex);
            }
            // If projectSizes doesn't contain the key, that probably means that a new project cache was generated
            // concurrently that isn't in the map that we had when the cache sizes were calculated.
            // Just skip it for now as it has no impact on our current disk space calculation.

            // Regardless of which case, we always want to move to the next index.
            oldestProjectIndex--;
        }

        // Convert to hash table for more efficient lookup.
        Collection<String> retainedProjectIDSet = new HashSet<>(retainedProjectIDs);

        Collection<File> cacheDirsToClean = cleanableCacheProjects.entrySet().stream()
            // Skip any projects that were designated for retention.
            .filter(entry -> !retainedProjectIDSet.contains(entry.getKey()))
            // Flatten to a stream of individual cache directories (for a specific project and a specific cache type)
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());

        for (var deleteMethod : getDeleteMethods().entrySet())
        {
            // Clean up just the directories that are under the specific cache type (i.e. fit, preview)
            deleteMethod.getValue().accept(cacheDirsToClean.stream()
                .filter(cacheDir -> Objects.equals(deleteMethod.getKey(), cacheDir.getParentFile()))
                .toArray(File[]::new));
        }
    }

    private static List<UUID> getRecentUUIDs(int numProjectsToKeep)
    {
        List<String> recentProjects = RecentProjects.getRecentProjectFilenames().stream().limit(numProjectsToKeep).collect(Collectors.toList());
        // Load view sets of recent projects to get their UUID.
        List<UUID> recentUUIDs = new ArrayList<>(recentProjects.size());
        for (String recentProject : recentProjects)
        {
            String projectName = recentProject.substring(recentProject.lastIndexOf(File.separator) + 1);
            File vsetFile = new File(recentProject + ".files" + File.separator + projectName + ".vset");

            if (vsetFile.exists()) // Might not exist anymore, in which case it's not considered recent.
            {
                try
                {
                    ViewSet viewSet = ViewSetReaderFromVSET.getInstance().readFromFile(vsetFile).finish();
                    recentUUIDs.add(viewSet.getUUID());
                }
                catch (IOException e)
                {
                    LOG.error("Failed to open project directory", e);
                }
            }

        }
        return recentUUIDs;
    }

    /**
     *
     * @param directory       The current directory whose size is being evaluated.
     * @param newProjectSizes Will be populated with updated individual project cache sizes.
     * @param projectID       The name of project whose cache size is being calculated.
     * @return The total size of the cache over all projects.
     */
    private long getDirectorySize(File directory, Map<String, Long> newProjectSizes, String projectID)
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
                    newProjectSizes.put(projectID, ((newProjectSizes.get(projectID) == null) ? 0 : newProjectSizes.get(projectID)) + file.length());
                }
                else
                {
                    if (projectID == null)
                    {
                        length += getDirectorySize(file, newProjectSizes, file.getName());
                    }
                    else
                    {
                        length += getDirectorySize(file, newProjectSizes, projectID);
                    }
                }
            }
        }
        LOG.debug("Directory size for {}: {}", directory, length);
        return length;
    }

    /**
     * Requests that the cache size be recalculated without any callback.
     * If a request is already running when this method is invoked, another request will not be started.
     */
    @Override
    public void requestCacheSizeRefresh()
    {
        requestCacheSizeRefresh(null, null);
    }

    /**
     * Requests that the cache size be recalculated.
     * If (and only if) the cache size is updated to a different value
     * (from this invocation or a parallel running invocation of the same method),
     * then the specified cache size callback will be invoked once and only once with the updated value.
     * If a request is already running when this method is invoked,
     * another request will not be started but the additional callback will instead be attached to the
     * already-running request and invoked if that request yields a different value than the current one.
     * If invoked, the callback will always be invoked from the JavaFX Application Thread.
     *
     * @param cacheSizeChangeCallback Runs only if the cache size changed.
     * @param onCompleteCallback      Always runs regardless of whether the cache size changed.  Also runs on the JavaFX thread.
     */
    protected void requestCacheSizeRefresh(DoubleConsumer cacheSizeChangeCallback, Runnable onCompleteCallback)
    {
        synchronized (cacheSizeCalcThreadLock)
        {
            addCacheSizeChangeCallback(cacheSizeChangeCallback);

            if (cacheSizeCalcThread == null) // only want one thread running at a time.
            {
                setCacheSizeCalcInProgress(true);
                cacheSizeCalcThread = new Thread(() ->
                {
                    try
                    {
                        Map<String, Long> newProjectSizes = new HashMap<>(getNumCachedProjects());

                        // Refresh the cache size.  This takes a while.
                        long newCacheSize = calcCacheSize(newProjectSizes);

                        // This will trigger the listener created above.
                        updateCacheSize(newCacheSize, newProjectSizes, onCompleteCallback);
                    }
                    catch (RuntimeException e)
                    {
                        LOG.error("Error calculating cache size", e);

                        synchronized (cacheSizeCalcThreadLock)
                        {
                            // Discard any listeners that didn't fire since an exception was thrown.
                            removeAllCacheSizeChangeCallbacks();

                            // If an exception occurs, we want to still note that the thread is done.
                            cacheSizeCalcThread = null;
                            setCacheSizeCalcInProgress(false);
                        }

                        // Callback that should always run regardless of whether the cache size changed or not.
                        if (onCompleteCallback != null)
                        {
                            onCompleteCallback.run();
                        }
                    }
                }, "Cache Size Calculation");

                cacheSizeCalcThread.start();
            }

            // If cache size calc thread is already running, then it should eventually trigger the listener registered above.
        }
    }

    /**
     *
     * @param postNewCacheSizes Handles actually posting the new cache size and individual project sizes,
     *                          guaranteed to be invoked from a synchronized context.
     */
    protected void synchronizedCacheSizeCalcComplete(Runnable postNewCacheSizes)
    {
        // prevent race condition if another call to requestCacheSizeRefresh comes in
        // while updating and cleaning up listeners.
        synchronized (cacheSizeCalcThreadLock)
        {
            try
            {
                // This should trigger any listeners attached to the property.
                postNewCacheSizes.run();
            }
            finally
            {
                // Discard any listeners that didn't fire (i.e. if the value didn't change).
                removeAllCacheSizeChangeCallbacks();

                // This thread is now done.
                // Set the thread reference to null to indicate that future refresh requests
                // should actually start a new thread.
                // We actually wait until after the JavaFX update via Platform.runLater
                // so that another thread doesn't start before the update is fully processed.
                cacheSizeCalcThread = null;
                setCacheSizeCalcInProgress(false);
            }
        }
    }

    /**
     *
     * @param newProjectSizes Will be populated with updated individual project cache sizes.
     * @return The total size of the cache over all projects.
     */
    private long calcCacheSize(Map<String, Long> newProjectSizes)
    {
        // Calculates both the total cache size as well as individual project sizes.
        return getCleanableCacheDirectories().stream()
            .mapToLong(dir -> getDirectorySize(dir, newProjectSizes, null))
            .sum();
    }

    /**
     * Gets the most cached projects in each cache (i.e. preview, fit).
     * Typically, each cache will have the same projects (or one might have a subset of the other).
     * Theoretically, it is possible that each cache has unique projects, in which case the total
     * after combining would be greater than what this method returns.
     * The intention is that this is to be used as an upper bound for the number of projects in an individual cache,
     * and will typically be close to, if not equal to, the number of projects in all caches combined.
     *
     * @return
     */
    protected int getNumCachedProjects()
    {
        return getCleanableCacheDirectories().stream()
            .mapToInt(dir -> Objects.requireNonNull(dir.listFiles()).length)
            .max().orElse(0);
    }

    protected boolean checkOldFilesExist()
    {
        Collection<File> cacheFiles = getCleanableCacheDirectories().stream()
            .flatMap(dir -> Arrays.stream(Objects.requireNonNull(dir.listFiles())))
            .collect(Collectors.toList());

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
}
