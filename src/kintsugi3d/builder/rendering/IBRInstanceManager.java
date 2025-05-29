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

package kintsugi3d.builder.rendering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.ViewSetWriterToVSET;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace.Builder;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.builder.state.*;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.gl.interactive.InitializationException;
import kintsugi3d.gl.interactive.InteractiveRenderable;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.util.EncodableColorImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public class IBRInstanceManager<ContextType extends Context<ContextType>> implements IOHandler, InteractiveRenderable<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(IBRInstanceManager.class);

    private final ContextType context;

    private boolean unloadRequested = false;
    private ViewSet loadedViewSet;
    private IBRInstance<ContextType> ibrInstance = null;
    private IBRInstance<ContextType> newInstance = null;
    private ProgressMonitor progressMonitor;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;
    private ReadonlySettingsModel settingsModel;
    private CameraViewListModel cameraViewListModel;

    private final List<Consumer<ViewSet>> viewSetLoadCallbacks
        = Collections.synchronizedList(new ArrayList<>());

    private final List<Consumer<IBRInstance<ContextType>>> instanceLoadCallbacks
        = Collections.synchronizedList(new ArrayList<>());

    private File loadedProjectFile;

    /**
     * Adds callbacks that will be invoked when the view set has finished loading (but before the GPU resources are loaded).
     * The callbacks will be cleared after being invoked.
     *
     * @param callback to add
     */
    @Override
    public void addViewSetLoadCallback(Consumer<ViewSet> callback)
    {
        synchronized (viewSetLoadCallbacks)
        {
            viewSetLoadCallbacks.add(callback);
        }
    }

    /**
     * Adds callbacks that will be invoked when the instance has finished loading.
     * The callbacks will be cleared after being invoked.
     *
     * @param callback to add
     */
    public void addInstanceLoadCallback(Consumer<IBRInstance<ContextType>> callback)
    {
        synchronized (instanceLoadCallbacks)
        {
            instanceLoadCallbacks.add(callback);
        }
    }

    public IBRInstanceManager(ContextType context)
    {
        this.context = context;
    }

    private void handleMissingFiles(Exception e)
    {
        log.error("An error occurred loading project: ", e);
        if (progressMonitor != null)
        {
            progressMonitor.fail(e);
        }
    }

    private void handleUserCancellation(UserCancellationException e)
    {
        log.info("Loading project was cancelled by user: ", e);
        if (progressMonitor != null)
        {
            progressMonitor.cancelComplete(e);
        }
    }

    @Override
    public boolean isInstanceLoaded()
    {
        return ibrInstance != null;
    }

    @Override
    public ViewSet getLoadedViewSet()
    {
        return loadedViewSet;
    }

    @Override
    public File getLoadedProjectFile()
    {
        return loadedProjectFile;
    }

    @Override
    public void setLoadedProjectFile(File loadedProjectFile)
    {
        this.loadedProjectFile = loadedProjectFile;
    }

    private void invokeViewSetLoadCallbacks(ViewSet viewSet)
    {
        synchronized (viewSetLoadCallbacks)
        {
            // Invoke callbacks
            for (Consumer<ViewSet> callback : viewSetLoadCallbacks)
            {
                callback.accept(viewSet);
            }

            // Clear the list of callbacks for the next load.
            viewSetLoadCallbacks.clear();
        }
    }

    private void loadInstance(String id, Builder<ContextType> builder) throws UserCancellationException
    {
        loadedViewSet = builder.getViewSet();

        List<File> imgFiles = loadedViewSet.getImageFiles();
        List<String> imgFileNames = new ArrayList<>();

        imgFiles.forEach(file->imgFileNames.add(file.getName()));

        MultithreadModels.getInstance().getCameraViewListModel().setCameraViewList(imgFileNames);

        // Invoke callbacks now that view set is loaded
        invokeViewSetLoadCallbacks(loadedViewSet);

        if(progressMonitor != null)
        {
            progressMonitor.setStageCount(2);
            progressMonitor.setStage(0, "Generating preview-resolution images...");
        }

        try
        {
            // Generate preview resolution images
            builder.generateUndistortedPreviewImages();
        }
        catch (IOException e)
        {
            log.error("One or more images failed to load", e);
        }

        // Create the instance (will be initialized on the graphics thread)
        IBRInstance<ContextType> newItem = new IBREngine<>(id, context, builder);

        newItem.getSceneModel().setObjectModel(this.objectModel);
        newItem.getSceneModel().setCameraModel(this.cameraModel);
        newItem.getSceneModel().setLightingModel(this.lightingModel);
        newItem.getSceneModel().setSettingsModel(this.settingsModel);
        newItem.getSceneModel().setCameraViewListModel(this.cameraViewListModel);

        newItem.setProgressMonitor(new ProgressMonitor()
        {
            @Override
            public void allowUserCancellation() throws UserCancellationException
            {
                if (progressMonitor != null)
                {
                    progressMonitor.allowUserCancellation();
                }
            }

            @Override
            public void cancelComplete(UserCancellationException e)
            {
                if (progressMonitor != null)
                {
                    progressMonitor.cancelComplete(e);
                }
            }

            @Override
            public void start()
            {
                if (progressMonitor != null)
                {
                    progressMonitor.start();
                }
            }

            @Override
            public void setProcessName(String processName) {
                if (progressMonitor != null)
                {
                    progressMonitor.setProcessName(processName);
                }
            }

            @Override
            public void setStageCount(int count)
            {
                if (progressMonitor != null)
                {
                    // Add one for the preview image generation step already completed.
                    progressMonitor.setStageCount(count + 1);
                }
            }

            @Override
            public void setStage(int stage, String message)
            {
                if (progressMonitor != null)
                {
                    // Add one for the preview image generation step already completed.
                    progressMonitor.setStage(stage + 1, message);
                }
            }

            @Override
            public void setMaxProgress(double maxProgress)
            {
                if (progressMonitor != null)
                {
                    progressMonitor.setMaxProgress(maxProgress);
                }
            }

            @Override
            public void setProgress(double progress, String message)
            {
                if (progressMonitor != null)
                {
                    progressMonitor.setProgress(progress, message);
                }
            }

            @Override
            public void complete()
            {
                newItem.getIBRResources().calibrateLightIntensities(false);
                newItem.reloadShaders();

                MenubarController.getInstance().setToggleableShaderDisable(!hasSpecularMaterials());

                if (progressMonitor != null)
                {
                    progressMonitor.complete();
                }
            }

            @Override
            public void fail(Throwable e)
            {
                if (progressMonitor != null)
                {
                    progressMonitor.fail(e);
                }
            }

            @Override
            public void warn(Throwable e)
            {
                if (progressMonitor != null)
                {
                    progressMonitor.warn(e);
                }
            }

            @Override
            public boolean isConflictingProcess() {
                return progressMonitor.isConflictingProcess();
            }
        });
        newInstance = newItem;
    }

    @Override
    public void loadFromVSETFile(String id, File vsetFile, File supportingFilesDirectory, ReadonlyLoadOptionsModel loadOptions)
    {
        if(this.progressMonitor.isConflictingProcess()){
            return;
        }

        this.progressMonitor.start();
        this.progressMonitor.setProcessName("Load from File");

        try
        {
            Builder<ContextType> contextTypeBuilder = IBRResourcesImageSpace.getBuilderForContext(this.context)
                .setProgressMonitor(this.progressMonitor)
                .setLoadOptions(loadOptions)
                .loadVSETFile(vsetFile, supportingFilesDirectory);

            loadInstance(id, contextTypeBuilder);
        }
        catch(UserCancellationException e)
        {
            handleUserCancellation(e);
        }
        catch (Exception e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void loadAgisoftFromZIP(MetashapeObjectChunk metashapeObjectChunk, ReadonlyLoadOptionsModel loadOptions) {

        // TODO There currently isn't functionality for a supportingFilesDirectory at this early in the process
        //  Restructuring required from Tetzlaff.

        if(this.progressMonitor.isConflictingProcess()){
            return;
        }

        this.progressMonitor.start();
        this.progressMonitor.setProcessName("Load from Agisoft Project");

        File supportingFilesDirectory = null;
        try {
            String orientationView = metashapeObjectChunk.getLoadPreferences().orientationViewName;
            double rotation = metashapeObjectChunk.getLoadPreferences().orientationViewRotateDegrees;

            Builder<ContextType> builder = IBRResourcesImageSpace.getBuilderForContext(this.context)
                    .setProgressMonitor(this.progressMonitor)
                    .setLoadOptions(loadOptions)
                    .loadAgisoftFromZIP(metashapeObjectChunk, supportingFilesDirectory)
                    .setOrientationView(orientationView, rotation);

            loadInstance(metashapeObjectChunk.getFramePath(), builder);
        }
        catch(UserCancellationException e)
        {
            handleUserCancellation(e);
        }
        catch (Exception e) {
            handleMissingFiles(e);
        }
    }

    @Override
    public void loadFromLooseFiles(String id, File xmlFile, File meshFile, File imageDirectory, boolean needsUndistort,
        String primaryViewName, double rotation, ReadonlyLoadOptionsModel loadOptions, UUID uuidOverride)
    {
        if(this.progressMonitor.isConflictingProcess()){
            return;
        }
        this.progressMonitor.start();
        this.progressMonitor.setProcessName("Load from loose files");

        try
        {
            Builder<ContextType> builder = IBRResourcesImageSpace.getBuilderForContext(this.context)
                .setProgressMonitor(this.progressMonitor)
                .setLoadOptions(loadOptions)
                .loadLooseFiles(xmlFile, meshFile, imageDirectory, needsUndistort, uuidOverride)
                .setOrientationView(primaryViewName, rotation);

            // Invoke callbacks now that view set is loaded
            loadInstance(id, builder);
        }
        catch(UserCancellationException e)
        {
            handleUserCancellation(e);
        }
        catch(Exception e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void requestFragmentShader(File shaderFile)
    {
        if (ibrInstance != null)
        {
            ibrInstance.getDynamicResourceManager().requestFragmentShader(shaderFile);
        }
    }

    @Override
    public void requestFragmentShader(File shaderFile, Map<String, Optional<Object>> extraDefines)
    {
        if (ibrInstance != null)
        {
            ibrInstance.getDynamicResourceManager().requestFragmentShader(shaderFile, extraDefines);
        }
    }

    public IBRInstance<ContextType> getLoadedInstance()
    {
        return ibrInstance;
    }

    @Override
    public void setProgressMonitor(ProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
    }

    @Override
    public DoubleUnaryOperator getLuminanceEncodingFunction()
    {
        if (ibrInstance != null)
        {
            return ibrInstance.getActiveViewSet().getLuminanceEncoding().encodeFunction;
        }
        else
        {
            // Default if no instance is loaded.
            return new SampledLuminanceEncoding().encodeFunction;
        }
    }

    @Override
    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        if (ibrInstance != null)
        {
            ibrInstance.getDynamicResourceManager().setTonemapping(linearLuminanceValues, encodedLuminanceValues);
        }
    }

    @Override
    public void applyLightCalibration()
    {
        if (ibrInstance != null)
        {
            ReadonlyViewSet viewSet = ibrInstance.getIBRResources().getViewSet();

            ibrInstance.getDynamicResourceManager().setLightCalibration(
                ibrInstance.getSceneModel().getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3());
        }
    }

    public boolean hasSpecularMaterials()
    {
        SpecularMaterialResources<ContextType> material = ibrInstance.getIBRResources().getSpecularMaterialResources();
        return material.getAlbedoMap() != null ||
            material.getSpecularRoughnessMap() != null ||
            material.getSpecularReflectivityMap() != null ||
            material.getORMMap() != null;
    }

    public void setCameraViewListModel(CameraViewListModel cameraViewListModel)
    {
        this.cameraViewListModel = cameraViewListModel;
        if (ibrInstance != null)
        {
            ibrInstance.getSceneModel().setCameraViewListModel(cameraViewListModel);
        }
    }

    public void setObjectModel(ReadonlyObjectModel objectModel)
    {
        this.objectModel = objectModel;
        if (ibrInstance != null)
        {
            ibrInstance.getSceneModel().setObjectModel(objectModel);
        }
    }

    public void setCameraModel(ReadonlyCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
        if (ibrInstance != null)
        {
            ibrInstance.getSceneModel().setCameraModel(cameraModel);
        }
    }

    public void setLightingModel(ReadonlyLightingModel lightingModel)
    {
        this.lightingModel = lightingModel;
        if (ibrInstance != null)
        {
            ibrInstance.getSceneModel().setLightingModel(lightingModel);
        }
    }

    public void setSettingsModel(ReadonlySettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
        if (ibrInstance != null)
        {
            ibrInstance.getSceneModel().setSettingsModel(settingsModel);
        }
    }

    @Override
    public Optional<EncodableColorImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        return ibrInstance.getDynamicResourceManager().loadEnvironmentMap(environmentMapFile);
    }

    @Override
    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        ibrInstance.getDynamicResourceManager().loadBackplate(backplateFile);
    }

    @Override
    public void saveToVSETFile(File vsetFile) throws IOException
    {
        ViewSetWriterToVSET.getInstance().writeToFile(loadedViewSet, vsetFile);
    }

    @Override
    public void saveMaterialFiles(File materialDirectory, Runnable finishedCallback)
    {
        if (ibrInstance == null || ibrInstance.getIBRResources() == null
            || ibrInstance.getIBRResources().getSpecularMaterialResources() == null)
        {
            if (finishedCallback != null)
            {
                finishedCallback.run();
            }
        }
        else
        {
            SpecularMaterialResources<ContextType> material
                = ibrInstance.getIBRResources().getSpecularMaterialResources();

            Rendering.runLater(() ->
            {
                material.saveAll(materialDirectory);

                if (finishedCallback != null)
                {
                    finishedCallback.run();
                }
            });
        }
    }

    @Override
    public void saveGlTF(File outputDirectory, ExportSettings settings)
    {
        if (ibrInstance != null)
        {
            ibrInstance.saveGlTF(outputDirectory, settings);
        }
    }

    @Override
    public void unload()
    {
        unloadRequested = true;
        loadedProjectFile = null;
    }

    @Override
    public void initialize() throws InitializationException
    {
        if (ibrInstance != null)
        {
            ibrInstance.initialize();
        }
    }

    @Override
    public void update()
    {
        if (unloadRequested)
        {
            if (ibrInstance != null)
            {
                ibrInstance.close();
                ibrInstance = null;
                loadedViewSet = null;
            }

            unloadRequested = false;
        }

        if (newInstance != null)
        {
            // If a new instance was just loaded, initialize it.
            try
            {
                newInstance.initialize();
            }
            catch (InitializationException e)
            {
                log.error("Error occurred initializing new instance:", e);

                newInstance.close();
                newInstance = null;
            }

            if (newInstance != null)
            {
                // Check for an old instance just to be safe
                if (ibrInstance != null)
                {
                    ibrInstance.close();
                }

                // Use the new instance as the active instance if initialization was successful
                ibrInstance = newInstance;

                newInstance = null;
            }

            // Invoke callbacks
            for (Consumer<IBRInstance<ContextType>> callback : instanceLoadCallbacks)
            {
                callback.accept(ibrInstance);
            }

            // Clear the list of callbacks for the next load.
            instanceLoadCallbacks.clear();
        }

        if (ibrInstance != null)
        {
            ibrInstance.update();
        }
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer)
    {
        if (ibrInstance != null)
        {
            ibrInstance.draw(framebuffer);
        }
    }

    @Override
    public void close()
    {
        if (ibrInstance != null)
        {
            ibrInstance.close();
        }
    }
}
