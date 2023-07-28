/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.io.ViewSetWriterToVSET;
import tetzlaff.ibrelight.javafx.internal.EnvironmentModelImpl;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;
import tetzlaff.interactive.InitializationException;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.ReadonlyObjectModel;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.AbstractImage;

public class IBRInstanceManager<ContextType extends Context<ContextType>> implements LoadingHandler, InteractiveRenderable<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(IBRInstanceManager.class);

    private final ContextType context;

    private boolean unloadRequested = false;
    private IBRInstance<ContextType> ibrInstance = null;
    private IBRInstance<ContextType> newInstance = null;
    private LoadingMonitor loadingMonitor;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;
    private ReadonlySettingsModel settingsModel;

    private final List<Consumer<IBRInstance<ContextType>>> instanceLoadCallbacks
        = Collections.synchronizedList(new ArrayList<>());

    /**
     * Adds callbacks that will be invoked when the instance has finished loading.
     * The callbacks will be cleared after being invoked.
     * @param callback to add
     */
    public void addInstanceLoadCallback(Consumer<IBRInstance<ContextType>> callback)
    {
        instanceLoadCallbacks.add(callback);
    }

    public IBRInstanceManager(ContextType context)
    {
        this.context = context;
    }

    private void handleMissingFiles(Exception e)
    {
        log.error("An error occurred:", e);
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
    public void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions)
    {
        this.loadingMonitor.startLoading();

        try
        {
            IBRInstance<ContextType> newItem =
                new IBREngine<>(id, context,
                    IBRResourcesImageSpace.getBuilderForContext(this.context)
                        .setLoadingMonitor(this.loadingMonitor)
                        .setLoadOptions(loadOptions)
                        .loadVSETFile(vsetFile));

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
            IBRInstance<ContextType> newItem =
                new IBREngine<>(id, context,
                    IBRResourcesImageSpace.getBuilderForContext(this.context)
                        .setLoadingMonitor(this.loadingMonitor)
                        .setLoadOptions(loadOptions)
                        .loadAgisoftFiles(xmlFile, meshFile, imageDirectory)
                        .setPrimaryView(primaryViewName));

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
        catch(Exception e)
        {
            handleMissingFiles(e);
        }
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
        ViewSetWriterToVSET.getInstance().writeToFile(ibrInstance.getActiveViewSet(), vsetFile);
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
