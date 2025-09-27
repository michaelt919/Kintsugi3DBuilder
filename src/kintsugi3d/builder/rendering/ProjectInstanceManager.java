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

import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.fit.decomposition.BasisImageCreator;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetWriterToVSET;
import kintsugi3d.builder.io.metashape.MetashapeChunk;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace.Builder;
import kintsugi3d.builder.resources.project.MissingImagesException;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.builder.state.CameraViewListModel;
import kintsugi3d.builder.state.cards.TabsManager;
import kintsugi3d.builder.state.scene.*;
import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.gl.interactive.InitializationException;
import kintsugi3d.gl.interactive.InteractiveRenderable;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.EncodableColorImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public class ProjectInstanceManager<ContextType extends Context<ContextType>> implements IOHandler, InteractiveRenderable<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectInstanceManager.class);

    private final ContextType context;

    private boolean unloadRequested = false;
    private ViewSet loadedViewSet;
    private ProjectInstance<ContextType> projectInstance = null;
    private ProjectInstance<ContextType> newInstance = null;
    private ProgressMonitor progressMonitor;

    private ReadonlyObjectPoseModel objectModel;
    private ReadonlyViewpointModel cameraModel;
    private ReadonlyLightingEnvironmentModel lightingModel;
    private ReadonlyGeneralSettingsModel settingsModel;
    private CameraViewListModel cameraViewListModel;

    private final List<Consumer<ViewSet>> viewSetLoadCallbacks
        = Collections.synchronizedList(new ArrayList<>(4));

    private final List<Consumer<ProjectInstance<ContextType>>> instanceLoadCallbacks
        = Collections.synchronizedList(new ArrayList<>(4));

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
    public void addInstanceLoadCallback(Consumer<ProjectInstance<ContextType>> callback)
    {
        synchronized (instanceLoadCallbacks)
        {
            instanceLoadCallbacks.add(callback);
        }
    }

    public ProjectInstanceManager(ContextType context)
    {
        this.context = context;
    }

    private void handleMissingFiles(Exception e)
    {
        LOG.error("An error occurred loading project: ", e);
        if (progressMonitor != null)
        {
            progressMonitor.fail(e);
        }
    }

    private void handleUserCancellation(UserCancellationException e)
    {
        LOG.info("Loading project was cancelled by user: ", e);
        if (progressMonitor != null)
        {
            progressMonitor.cancelComplete(e);
        }
    }

    @Override
    public boolean isInstanceLoaded()
    {
        return projectInstance != null;
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
        List<String> imgFileNames = new ArrayList<>(imgFiles.size());

        imgFiles.forEach(file -> imgFileNames.add(file.getName()));

        Global.state().getCameraViewListModel().setCameraViewList(imgFileNames);

        // Invoke callbacks now that view set is loaded
        invokeViewSetLoadCallbacks(loadedViewSet);

        if (progressMonitor != null)
        {
            progressMonitor.setStageCount(2);
            progressMonitor.setStage(0, "Generating preview-resolution images...");
        }

        try
        {
            // Generate preview resolution images and thumbnails
            builder.generateAllPreviewImages();
        }
        catch (IOException e)
        {
            LOG.error("One or more images failed to load", e);
        }

        // Create the instance (will be initialized on the graphics thread)
        ProjectInstance<ContextType> newItem = new ProjectRenderingEngine<>(id, context, builder);

        newItem.getSceneModel().setObjectModel(this.objectModel);
        newItem.getSceneModel().setCameraModel(this.cameraModel);
        newItem.getSceneModel().setLightingModel(this.lightingModel);
        newItem.getSceneModel().setSettingsModel(this.settingsModel);
        newItem.getSceneModel().setCameraViewListModel(this.cameraViewListModel);

        newItem.setProgressMonitor(new BackendProgressMonitor(newItem, progressMonitor));
        newInstance = newItem;

        new TabsManager(loadedViewSet, newInstance).rebuildTabs();
    }

    @Override
    public void loadFromVSETFile(String id, File vsetFile, File supportingFilesDirectory, ReadonlyLoadOptionsModel loadOptions)
    {
        if (this.progressMonitor.isConflictingProcess())
        {
            return;
        }

        this.progressMonitor.start();
        this.progressMonitor.setProcessName("Load from File");

        try
        {
            Builder<ContextType> contextTypeBuilder = GraphicsResourcesImageSpace.getBuilderForContext(this.context)
                .setProgressMonitor(this.progressMonitor)
                .setImageLoadOptions(loadOptions)
                .loadVSETFile(vsetFile, supportingFilesDirectory);

            loadInstance(id, contextTypeBuilder);
        }
        catch (UserCancellationException e)
        {
            handleUserCancellation(e);
        }
        catch (Exception e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void loadFromMetashapeModel(MetashapeModel model, ReadonlyLoadOptionsModel loadOptionsModel)
    {

        if (this.progressMonitor.isConflictingProcess())
        {
            return;
        }

        this.progressMonitor.start();
        this.progressMonitor.setProcessName("Load from Agisoft Project");

        try
        {
            MetashapeChunk parentChunk = model.getChunk();
            String orientationView = model.getLoadPreferences().orientationViewName;
            double rotation = model.getLoadPreferences().orientationViewRotateDegrees;

            Builder<ContextType> builder = GraphicsResourcesImageSpace.getBuilderForContext(this.context)
                .setProgressMonitor(this.progressMonitor)
                .setImageLoadOptions(loadOptionsModel)
                .loadFromMetashapeModel(model)
                .setOrientationView(orientationView, rotation);

            loadInstance(parentChunk.getFramePath(), builder);
        }
        catch (UserCancellationException e)
        {
            handleUserCancellation(e);
        }
        catch (MissingImagesException | IOException | XMLStreamException e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void loadFromLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions, ReadonlyLoadOptionsModel imageLoadOptions)
    {
        if (this.progressMonitor.isConflictingProcess())
        {
            return;
        }
        this.progressMonitor.start();
        this.progressMonitor.setProcessName("Load from loose files");

        try
        {
            Builder<ContextType> builder = GraphicsResourcesImageSpace.getBuilderForContext(this.context)
                .setProgressMonitor(this.progressMonitor)
                .setImageLoadOptions(imageLoadOptions)
                .loadLooseFiles(xmlFile, viewSetLoadOptions);

            // Invoke callbacks now that view set is loaded
            loadInstance(id, builder);
        }
        catch (UserCancellationException e)
        {
            handleUserCancellation(e);
        }
        catch (Exception e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void requestFragmentShader(File shaderFile)
    {
        if (projectInstance != null)
        {
            projectInstance.getDynamicResourceManager().requestFragmentShader(shaderFile);
        }
    }

    @Override
    public void requestFragmentShader(File shaderFile, Map<String, Optional<Object>> extraDefines)
    {
        if (projectInstance != null)
        {
            projectInstance.getDynamicResourceManager().requestFragmentShader(shaderFile, extraDefines);
        }
    }

    @Override
    public void requestFragmentShader(UserShader userShader)
    {
        requestFragmentShader(new File("shaders", userShader.getFilename()), userShader.getDefines());
    }

    public ProjectInstance<ContextType> getLoadedInstance()
    {
        return projectInstance;
    }

    @Override
    public void setProgressMonitor(ProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
    }

    @Override
    public DoubleUnaryOperator getLuminanceEncodingFunction()
    {
        if (projectInstance != null)
        {
            return projectInstance.getActiveViewSet().getLuminanceEncoding().encodeFunction;
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
        if (projectInstance != null)
        {
            projectInstance.getDynamicResourceManager().setTonemapping(linearLuminanceValues, encodedLuminanceValues);
        }
    }

    @Override
    public void clearTonemapping()
    {
        if (projectInstance != null)
        {
            projectInstance.getDynamicResourceManager().clearTonemapping();
        }
    }

    @Override
    public void requestLightIntensityCalibration()
    {
        if (projectInstance != null)
        {
            Rendering.runLater(() -> projectInstance.getResources().calibrateLightIntensities());
        }
    }

    @Override
    public void applyLightCalibration()
    {
        if (projectInstance != null)
        {
            projectInstance.getDynamicResourceManager().setLightCalibration(
                projectInstance.getSceneModel().getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3());
        }
    }

    public void setCameraViewListModel(CameraViewListModel cameraViewListModel)
    {
        this.cameraViewListModel = cameraViewListModel;
        if (projectInstance != null)
        {
            projectInstance.getSceneModel().setCameraViewListModel(cameraViewListModel);
        }
    }

    public void setObjectModel(ReadonlyObjectPoseModel objectModel)
    {
        this.objectModel = objectModel;
        if (projectInstance != null)
        {
            projectInstance.getSceneModel().setObjectModel(objectModel);
        }
    }

    public void setCameraModel(ReadonlyViewpointModel cameraModel)
    {
        this.cameraModel = cameraModel;
        if (projectInstance != null)
        {
            projectInstance.getSceneModel().setCameraModel(cameraModel);
        }
    }

    public void setLightingModel(ReadonlyLightingEnvironmentModel lightingModel)
    {
        this.lightingModel = lightingModel;
        if (projectInstance != null)
        {
            projectInstance.getSceneModel().setLightingModel(lightingModel);
        }
    }

    public void setUserShaderModel(ReadonlyUserShaderModel userShaderModel)
    {
        userShaderModel.registerHandler(this::requestFragmentShader);
    }

    public void setSettingsModel(ReadonlyGeneralSettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
        if (projectInstance != null)
        {
            projectInstance.getSceneModel().setSettingsModel(settingsModel);
        }
    }

    @Override
    public Optional<EncodableColorImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        return projectInstance.getDynamicResourceManager().loadEnvironmentMap(environmentMapFile);
    }

    @Override
    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        projectInstance.getDynamicResourceManager().loadBackplate(backplateFile);
    }

    @Override
    public void saveToVSETFile(File vsetFile) throws IOException
    {
        ViewSetWriterToVSET.getInstance().writeToFile(loadedViewSet, vsetFile);
    }

    @Override
    public void saveAllMaterialFiles(File materialDirectory, Runnable finishedCallback)
    {
        if (projectInstance == null || projectInstance.getResources() == null
            || projectInstance.getResources().getSpecularMaterialResources() == null)
        {
            if (finishedCallback != null)
            {
                finishedCallback.run();
            }
        }
        else
        {
            SpecularMaterialResources<ContextType> material
                = projectInstance.getResources().getSpecularMaterialResources();

            Rendering.runLater(() ->
            {
                material.saveAll(materialDirectory);

                // Save basis image visualization for cards
                try (BasisImageCreator<ContextType> basisImageCreator =
                         new BasisImageCreator<>(material.getContext(), material.getBasisResources().getBasisResolution()))
                {
                    basisImageCreator.createImages(material, loadedViewSet.getThumbnailImageDirectory());
                }
                catch (IOException e)
                {
                    ExceptionHandling.error("Error saving basis image thumbnails:", e);
                }

                if (finishedCallback != null)
                {
                    finishedCallback.run();
                }
            });
        }
    }

    @Override
    public void saveGLTF(File outputDirectory, ExportSettings settings)
    {
        if (projectInstance != null)
        {
            projectInstance.saveGLTF(outputDirectory, settings);
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
        if (projectInstance != null)
        {
            projectInstance.initialize();
        }
    }

    @Override
    public void update()
    {
        if (unloadRequested)
        {
            if (projectInstance != null)
            {
                projectInstance.close();
                projectInstance = null;
                loadedViewSet = null;

                Global.state().getProjectModel().setProjectLoaded(false);
                Global.state().getProjectModel().setProjectProcessed(false);
                Global.state().getProjectModel().setProcessedTextureResolution(0);
                Global.state().getProjectModel().setModelSize(new Vector3(1.0f));

                // Empty sidebar; will be repopulated when another project is opened.
                Global.state().getTabModels().clearTabs();
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
                LOG.error("Error occurred initializing new instance:", e);

                newInstance.close();
                newInstance = null;
            }

            if (newInstance != null)
            {
                // Check for an old instance just to be safe
                if (projectInstance != null)
                {
                    projectInstance.close();
                }

                // Use the new instance as the active instance if initialization was successful
                projectInstance = newInstance;

                newInstance = null;
            }

            // Invoke callbacks
            for (Consumer<ProjectInstance<ContextType>> callback : instanceLoadCallbacks)
            {
                callback.accept(projectInstance);
            }

            // Clear the list of callbacks for the next load.
            instanceLoadCallbacks.clear();
        }

        if (projectInstance != null)
        {
            projectInstance.update();
        }
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer)
    {
        if (projectInstance != null)
        {
            projectInstance.draw(framebuffer);
        }
    }

    @Override
    public void close()
    {
        if (projectInstance != null)
        {
            projectInstance.close();

            Global.state().getProjectModel().setProjectOpen(false);
            Global.state().getProjectModel().clearProjectName();
            Global.state().getProjectModel().setProjectLoaded(false);
            Global.state().getProjectModel().setProjectProcessed(false);
            Global.state().getProjectModel().setProcessedTextureResolution(0);
            Global.state().getProjectModel().setModelSize(new Vector3(1.0f));
        }
    }
}
