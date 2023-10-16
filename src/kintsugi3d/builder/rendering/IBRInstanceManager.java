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

package kintsugi3d.builder.rendering;

import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.ViewSetWriterToVSET;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace.Builder;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.builder.state.ReadonlyCameraModel;
import kintsugi3d.builder.state.ReadonlyLightingModel;
import kintsugi3d.builder.state.ReadonlyObjectModel;
import kintsugi3d.builder.state.ReadonlySettingsModel;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.gl.interactive.InitializationException;
import kintsugi3d.gl.interactive.InteractiveRenderable;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.AbstractImage;
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
    private LoadingMonitor loadingMonitor;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;
    private ReadonlySettingsModel settingsModel;

    private final List<Consumer<ViewSet>> viewSetLoadCallbacks
        = Collections.synchronizedList(new ArrayList<>());

    private final List<Consumer<IBRInstance<ContextType>>> instanceLoadCallbacks
        = Collections.synchronizedList(new ArrayList<>());

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
        log.error("An error occurred loading project:", e);
        if (loadingMonitor != null)
        {
            loadingMonitor.loadingFailed(e);
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

    private void loadInstance(String id, Builder<ContextType> builder) throws IOException
    {
        loadedViewSet = builder.getViewSet();

        // Invoke callbacks now that view set is loaded
        invokeViewSetLoadCallbacks(loadedViewSet);

        // Kick off request to generate preview resolution images
        builder.generateUndistortedPreviewImages();

        // Create the instance (will be initialized on the graphics thread)
        IBRInstance<ContextType> newItem = new IBREngine<>(id, context, builder);

        newItem.getSceneModel().setObjectModel(this.objectModel);
        newItem.getSceneModel().setCameraModel(this.cameraModel);
        newItem.getSceneModel().setLightingModel(this.lightingModel);
        newItem.getSceneModel().setSettingsModel(this.settingsModel);

        newItem.setLoadingMonitor(new LoadingMonitor()
        {
            @Override
            public void startLoading()
            {
                if (loadingMonitor != null)
                {
                    loadingMonitor.startLoading();
                }
            }

            @Override
            public void setMaximum(double maximum)
            {
                if (loadingMonitor != null)
                {
                    loadingMonitor.setMaximum(maximum);
                }
            }

            @Override
            public void setProgress(double progress)
            {
                if (loadingMonitor != null)
                {
                    loadingMonitor.setProgress(progress);
                }
            }

            @Override
            public void loadingComplete()
            {
                double primaryViewDistance = newItem.getIBRResources().getPrimaryViewDistance();
                Vector3 lightIntensity = new Vector3((float)(primaryViewDistance * primaryViewDistance));

                newItem.getIBRResources().initializeLightIntensities(lightIntensity, false);
                newItem.reloadShaders();

                if (loadingMonitor != null)
                {
                    loadingMonitor.loadingComplete();
                }
            }

            @Override
            public void loadingFailed(Exception e)
            {
                if (loadingMonitor != null)
                {
                    loadingMonitor.loadingFailed(e);
                }
            }
        });
        newInstance = newItem;
    }

    @Override
    public void loadFromVSETFile(String id, File vsetFile, File supportingFilesDirectory, ReadonlyLoadOptionsModel loadOptions)
    {
        this.loadingMonitor.startLoading();

        try
        {
            Builder<ContextType> contextTypeBuilder = IBRResourcesImageSpace.getBuilderForContext(this.context)
                .setLoadingMonitor(this.loadingMonitor)
                .setLoadOptions(loadOptions)
                .loadVSETFile(vsetFile, supportingFilesDirectory);

            loadInstance(id, contextTypeBuilder);
        }
        catch (Exception e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File imageDirectory, String primaryViewName,
        ReadonlyLoadOptionsModel loadOptions)
    {
        this.loadingMonitor.startLoading();

        try
        {
            Builder<ContextType> builder = IBRResourcesImageSpace.getBuilderForContext(this.context)
                .setLoadingMonitor(this.loadingMonitor)
                .setLoadOptions(loadOptions)
                .loadAgisoftFiles(xmlFile, meshFile, imageDirectory)
                .setPrimaryView(primaryViewName);

            // Invoke callbacks now that view set is loaded
            loadInstance(id, builder);
        }
        catch(Exception e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void requestFragmentShader(File shaderFile)
    {
        ibrInstance.getDynamicResourceManager().requestFragmentShader(shaderFile);
    }

    public IBRInstance<ContextType> getLoadedInstance()
    {
        return ibrInstance;
    }

    @Override
    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
    }

    @Override
    public DoubleUnaryOperator getLuminanceEncodingFunction()
    {
        return ibrInstance.getActiveViewSet().getLuminanceEncoding().encodeFunction;
    }

    @Override
    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        ibrInstance.getDynamicResourceManager().setTonemapping(linearLuminanceValues, encodedLuminanceValues);
    }

    @Override
    public void applyLightCalibration()
    {
        ReadonlyViewSet viewSet = ibrInstance.getIBRResources().getViewSet();

        ibrInstance.getDynamicResourceManager().setLightCalibration(
            viewSet.getLightPosition(viewSet.getLightIndex(viewSet.getPrimaryViewIndex()))
                .plus(ibrInstance.getSceneModel().getSettingsModel().get("currentLightCalibration", Vector2.class)
                        .asVector3()));
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
    public Optional<AbstractImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
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
            || ibrInstance.getIBRResources().getSpecularMaterialResources() != null)
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
