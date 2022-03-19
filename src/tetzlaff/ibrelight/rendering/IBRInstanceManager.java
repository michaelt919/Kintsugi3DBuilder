/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering;

import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.*;
import tetzlaff.interactive.InitializationException;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.ReadonlyObjectModel;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.AbstractImage;

public class IBRInstanceManager<ContextType extends Context<ContextType>> implements LoadingHandler, InteractiveRenderable<ContextType>
{
    private final ContextType context;

    private IBRInstance<ContextType> ibrInstance = null;
    private IBRInstance<ContextType> newInstance = null;
    private LoadingMonitor loadingMonitor;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;
    private ReadonlySettingsModel settingsModel;

    public IBRInstanceManager(ContextType context)
    {
        this.context = context;
    }

    private void handleMissingFiles(Exception e)
    {
        e.printStackTrace();
        if (loadingMonitor != null)
        {
            loadingMonitor.loadingFailed(e);
        }
    }

    @Override
    public void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions)
    {
        this.loadingMonitor.startLoading();

        try
        {
            IBRInstance<ContextType> newItem =
                new IBREngine<>(id, context,
                    IBRResources.getBuilderForContext(this.context)
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

                    for (int i = 0; i < newItem.getActiveViewSet().getLightCount(); i++)
                    {
                        if (Objects.equals(newItem.getActiveViewSet().getLightIntensity(i), Vector3.ZERO))
                        {
                            newItem.getActiveViewSet().setLightIntensity(i, lightIntensity);
                        }
                    }

                    newItem.getActiveViewSet().setInfiniteLightSources(false);
                    newItem.getIBRResources().updateLightData();
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
        catch (FileNotFoundException e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, String primaryViewName,
        ReadonlyLoadOptionsModel loadOptions)
    {
        this.loadingMonitor.startLoading();

        try
        {
            IBRInstance<ContextType> newItem =
                new IBREngine<>(id, context,
                    IBRResources.getBuilderForContext(this.context)
                        .setLoadingMonitor(this.loadingMonitor)
                        .setLoadOptions(loadOptions)
                        .loadAgisoftFiles(xmlFile, meshFile, undistortedImageDirectory)
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

                    for (int i = 0; i < newItem.getActiveViewSet().getLightCount(); i++)
                    {
                        newItem.getActiveViewSet().setLightIntensity(i, lightIntensity);
                    }

                    newItem.getActiveViewSet().setInfiniteLightSources(false);

                    newItem.getIBRResources().updateLightData();
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
        }
        catch(FileNotFoundException|XMLStreamException e)
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
        ViewSet viewSet = ibrInstance.getIBRResources().viewSet;

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
        try (OutputStream stream = new FileOutputStream(vsetFile))
        {
            ibrInstance.getActiveViewSet().writeVSETFileToStream(stream, vsetFile.getParentFile().toPath());
        }
    }

    @Override
    public void unload()
    {
        if (ibrInstance != null)
        {
            ibrInstance.close();
            ibrInstance = null;
        }
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
        if (newInstance != null)
        {
            // If a new instance was just loaded, initialize it.
            try
            {
                newInstance.initialize();

                // Check for an old instance just to be safe
                unload();

                // Use the new instance as the active instance if initialization was successful
                ibrInstance = newInstance;
            }
            catch (InitializationException e)
            {
                e.printStackTrace();
            }

            newInstance = null;
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
