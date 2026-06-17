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

package kintsugi3d.builder.rendering;

import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetWriterToVSET;
import kintsugi3d.builder.io.metashape.MetashapeChunk;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.rendering.components.RenderingSubject;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace.Builder;
import kintsugi3d.builder.resources.project.MeshImportException;
import kintsugi3d.builder.resources.project.MissingImagesException;
import kintsugi3d.builder.state.CameraViewListModel;
import kintsugi3d.builder.state.cards.TabsManager;
import kintsugi3d.builder.state.scene.*;
import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;
import kintsugi3d.gl.builders.framebuffer.DoubleFramebufferFactory;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.interactive.InitializationException;
import kintsugi3d.gl.interactive.InteractiveRenderableBase;
import kintsugi3d.gl.interactive.RefreshableCollection;
import kintsugi3d.gl.interactive.RenderRefreshable;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.window.FramebufferCanvas;
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

public class ProjectInstanceManager<ContextType extends Context<ContextType>>
    extends InteractiveRenderableBase<ContextType> implements IOHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectInstanceManager.class);

    private final ContextType context;

    private final RefreshableCollection<RenderRefreshable<ContextType>> renderViews = new RefreshableCollection<>();
    private final Map<UserShader, RenderRefreshable<ContextType>> renderViewMap = new HashMap<>(8);

    private ViewSet loadedViewSet;
    private ProjectInstance<ContextType> projectInstance;
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

    private void handleMeshImportException(MeshImportException e)
    {
        LOG.error("An error occurred loading model: ", e);
        if (progressMonitor != null)
        {
            progressMonitor.fail(e);
        }
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
        ViewSet newViewSet = builder.getViewSet();
        int cameraCount = newViewSet.getCombinedCameraPoseCount();
        if (cameraCount > 1024 && progressMonitor != null)
        {
            IOException e = new IOException(String.format("Dataset has %d cameras, which exceeds 1024 and may fail on many graphics cards.", cameraCount));
            progressMonitor.warn(e);
        }
        boolean hasUnsupportedCorrections = newViewSet.hasUnsupportedCorrections();
        if (hasUnsupportedCorrections && progressMonitor != null)
        {
            IOException e = new IOException("This project uses 'Fit additional corrections' which are not supported.");
            progressMonitor.warn(e);
        }

        List<File> imgFiles = newViewSet.getAllImageFiles();
        List<String> imgFileNames = new ArrayList<>(imgFiles.size());

        imgFiles.forEach(file -> imgFileNames.add(file.getName()));

        Global.state().getCameraViewListModel().setCameraViewList(imgFileNames);

        // Invoke callbacks now that view set is loaded
        invokeViewSetLoadCallbacks(newViewSet);

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
        ProjectInstance<ContextType> newInstance = new ProjectRenderingEngine<>(id, context, builder);
        newInstance.setOwningApp(this.getOwningApp());

        initializeSceneModel(newInstance.getSceneModel());

        newInstance.setProgressMonitor(new BackendProgressMonitor(newInstance, progressMonitor));

        // Use the runLater system so that the rendering loop knows that an operation that might take longer is queued.
        Rendering.runLater(() ->
        {
            // Wait to actually set the loaded view set until we're about to actually initialize the corresponding instance.
            loadedViewSet = newViewSet;

            // Wait to refresh the tabs manager until we're about to actually initialize the new instance.
            new TabsManager(newViewSet, newInstance).rebuildTabs();

            // If a new instance was just loaded, initialize it.
            try
            {
                newInstance.initialize();

                // Check for an old instance just to be safe
                if (projectInstance != null)
                {
                    projectInstance.close();
                }

                // Use the new instance as the active instance if initialization was successful
                projectInstance = newInstance;
            }
            catch (InitializationException e)
            {
                LOG.error("Error occurred initializing new instance:", e);
                newInstance.close();
            }

            // Invoke callbacks
            for (Consumer<ProjectInstance<ContextType>> callback : instanceLoadCallbacks)
            {
                callback.accept(projectInstance);
            }

            // Clear the list of callbacks for the next load.
            instanceLoadCallbacks.clear();

            // Update once before drawing
            newInstance.update();

            // Force GPU resources to be fully loaded / flushed by rendering once to a small throwaway FBO.
            try(FramebufferObject<ContextType> tempFBO = context.buildFramebufferObject(256, 256)
                .addColorAttachment()
                .addDepthAttachment()
                .createFramebufferObject())
            {
                newInstance.draw(tempFBO);
            }
        });
    }

    private void initializeSceneModel(SceneModel sceneModel)
    {
        sceneModel.setObjectModel(this.objectModel);
        sceneModel.setCameraModel(this.cameraModel);
        sceneModel.setLightingModel(this.lightingModel);
        sceneModel.setSettingsModel(this.settingsModel);
        sceneModel.setCameraViewListModel(this.cameraViewListModel);
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
        catch (MeshImportException e)
        {
            handleMeshImportException(e);
        }
        catch (IOException|RuntimeException e)
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
            String orientationView = model.getLoadPreferences().getOrientationViewName();
            double rotation = model.getLoadPreferences().getOrientationViewRotateDegrees();

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
        catch (MeshImportException e)
        {
            handleMeshImportException(e);
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

    public RefreshableCollection<RenderRefreshable<ContextType>> getRenderViews()
    {
        return renderViews;
    }

    public void addRenderView(UserShader shader, FramebufferSize initialSize,
                              Consumer<FramebufferCanvas<ContextType>> framebufferCallback)
    {
        // Create a new rendering engine instance that references the same resources as the main rendering engine.
        // This can run on any thread, but initialization needs to run on the graphics thread.
        ProjectRenderingEngine<ContextType> renderView =
            new ProjectRenderingEngine<>(projectInstance.getID(), context, projectInstance.getResources());
        initializeSceneModel(renderView.getSceneModel());

        Rendering.runLater(() ->
        {
            // Create framebuffer
            DoubleFramebufferObject<ContextType> framebuffer =
                DoubleFramebufferFactory.create(context, initialSize.width, initialSize.height);

            // Create and initialize refreshable, which will manage the framebuffer object
            RenderRefreshable<ContextType> refreshable = RenderRefreshable.createWithManagedFrambufferObject(
                context, renderView, framebuffer);

            try
            {
                // Pre-initialize here so that we can set the shader right away.
                // RefreshableCollection will just skip this one when processing its initialize queue
                // as it sees that it is already initialized.
                refreshable.initialize();
            }
            catch (InitializationException e)
            {
                LOG.error("Error initialing render view for shader: {}", shader.getFriendlyName(), e);
            }

            // Set to use the specified shader.
            RenderingSubject<ContextType> subject = renderView.getSubject();
            subject.setExtraFragmentShaderDefines(shader.getDefines());
            subject.useFragmentShader(shader.getFile());

            // render views will defer actually adding it to its main list until its own refresh call
            // so it can safely be added here in the processing of the runLater queue.
            renderViews.add(refreshable);

            // Also store it in a map so that we can find it by UserShader when it needs to be removed.
            renderViewMap.put(shader, refreshable);

            framebufferCallback.accept(FramebufferCanvas.createUsingExistingFramebuffer(framebuffer));
        });
    }

    public void removeRenderView(UserShader shader)
    {
        RenderRefreshable<ContextType> renderViewToRemove = renderViewMap.get(shader);

        if (renderViewToRemove != null)
        {
            // This adds the render view to the refreshable collection's terminate queue so it will get cleaned up.
            renderViews.remove(renderViewToRemove);
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
        requestFragmentShader(userShader.getFile(), userShader.getDefines());
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
            || projectInstance.getResources().getTextureResources() == null)
        {
            if (finishedCallback != null)
            {
                finishedCallback.run();
            }
        }
        else
        {
            Rendering.runLater(() ->
            {
                projectInstance.getResources().getTextureResources().saveAll(materialDirectory);

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
        loadedProjectFile = null;

        // Also remove render views which will be tied to the loaded project.
        renderViewMap.clear();
        renderViews.clear();

        // Use the runLater system so that the rendering loop knows that an operation that might take longer is queued.
        Rendering.runLater(() ->
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
        });
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
