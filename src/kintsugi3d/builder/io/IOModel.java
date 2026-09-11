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

package kintsugi3d.builder.io;

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.viewset.View;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.io.metashape.MetashapeTextures;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.rendering.RenderableInstance;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.gl.material.ImportedMaterial;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.util.EncodableColorImage;
import kintsugi3d.util.UnzipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public class IOModel
{
    private static final Logger LOG = LoggerFactory.getLogger(IOModel.class);

    private IOHandler handler;
    private final AggregateProgressMonitor progressMonitor = new AggregateProgressMonitor();
    private ReadonlyLoadOptionsModel imageLoadOptionsModel;

    private File loadedProjectFile;
    private File loadedViewSetFile;

    public ProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    public IOHandler getLoadingHandler()
    {
        return handler;
    }

    public void setLoadingHandler(IOHandler handler)
    {
        this.handler = handler;
        this.handler.setProgressMonitor(progressMonitor);
    }

    public void addProgressMonitor(ProgressMonitor monitor)
    {
        this.progressMonitor.addSubMonitor(monitor);
    }

    public void setImageLoadOptionsModel(ReadonlyLoadOptionsModel imageLoadOptionsModel)
    {
        this.imageLoadOptionsModel = imageLoadOptionsModel;
    }

    public RenderableInstance<?> getRenderableForShader(UserShader shader)
    {
        return this.handler.getRenderableForShader(shader);
    }

    public void addViewSetLoadCallback(Runnable callback)
    {
        this.handler.addViewSetLoadCallback(callback);
    }

    public void addViewSetLoadCallback(Consumer<ViewSet> callback)
    {
        this.handler.addViewSetLoadCallback(callback);
    }

    public void addMainRenderableLoadCallback(Consumer<RenderableInstance<?>> callback)
    {
        this.handler.addMainRenderableLoadCallback(callback);
    }

    public ViewSet getLoadedViewSet()
    {
        return this.handler.getLoadedViewSet();
    }

    public RenderableInstance<?> getMainRenderable()
    {
        return this.handler.getMainRenderable();
    }

    public File getLoadedViewSetFile()
    {
        return loadedViewSetFile;
    }

    public File getLoadedProjectFile()
    {
        return loadedProjectFile;
    }

    private void load(Runnable loader)
    {
        load("(Untitled)", loader);
    }

    private void load(String projectName, Runnable loader)
    {
        unload(() ->
        {
            ProjectModel projectModel = Global.state().getProjectModel();
            projectModel.setProjectOpen(true);
            projectModel.setProjectName(projectName);

            new Thread(loader, "Loading Thread").start();
        });
    }

    public void loadFromLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions)
    {
        load(() -> this.handler.loadFromLooseFiles(id, xmlFile, viewSetLoadOptions, imageLoadOptionsModel));
    }

    public void hotSwapLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions)
    {
        load(() ->
        {
            viewSetLoadOptions.uuid = getLoadedViewSet() != null ? getLoadedViewSet().getUUID() : null;
            this.handler.loadFromLooseFiles(id, xmlFile, viewSetLoadOptions, imageLoadOptionsModel);
        });
    }

    public void loadFromMetashapeModel(MetashapeModel model)
    {
        load(() -> this.handler.loadFromMetashapeModel(model, imageLoadOptionsModel));
    }

    public void loadExistingProject(File projectFile)
    {
        //need to check for conflicting process early so crucial info isn't unloaded
        if (progressMonitor.isConflictingProcess())
        {
            return;
        }

        //open the project, update the recent files list & recentDirectory, disable shaders which aren't useful until processing textures
        RecentProjects.setMostRecentDirectory(projectFile.getParentFile());

        File vsetFile;

        ProjectModel projectModel = Global.state().getProjectModel();
        if (projectFile.getName().endsWith(".vset"))
        {
            vsetFile = projectFile;
        }
        else
        {
            try
            {
                vsetFile = projectModel.openProjectFile(projectFile);
            }
            catch (RuntimeException | IOException | SAXException | ParserConfigurationException e)
            {
                ExceptionHandling.error("An error occurred opening project", e);
                vsetFile = null;
            }
        }

        loadExistingProject(projectFile, vsetFile);
    }

    private void loadExistingProject(File projectFile, File vsetFile)
    {
        if (vsetFile != null)
        {
            load(projectFile.getName(), () ->
            {
                try
                {
                    this.loadedProjectFile = projectFile;
                    this.loadedViewSetFile = vsetFile;

                    RecentProjects.addToRecentFiles(projectFile.getAbsolutePath());

                    if (Objects.equals(projectFile.getParentFile(), vsetFile.getParentFile()))
                    {
                        // VSET file is the project file or they're in the same directory.
                        // Use a supporting files directory underneath by default
                        File supportingFilesDirectory = getDefaultSupportingFilesDirectory(projectFile);
                        this.handler.loadFromVSETFile(vsetFile.getPath(), vsetFile, supportingFilesDirectory, imageLoadOptionsModel);
                    }
                    else
                    {
                        // VSET file is presumably already in a supporting files directory, so just use that directory by default
                        this.handler.loadFromVSETFile(vsetFile.getPath(), vsetFile, vsetFile.getParentFile(), imageLoadOptionsModel);
                    }
                }
                catch (RuntimeException e)
                {
                    LOG.error("Error loading project", e);
                }
                catch (Error e)
                {
                    LOG.error("Error loading project", e);
                    //noinspection ProhibitedExceptionThrown
                    throw e;
                }
            });

            // TODO might be some edge case issue here if the tone calibration window was already open (based on old TODO comment)?
        }
    }

    public Optional<EncodableColorImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        return this.handler.loadEnvironmentMap(environmentMapFile);
    }

    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        this.handler.loadBackplate(backplateFile);
    }

    private static File getDefaultSupportingFilesDirectory(File projectFile)
    {
        return new File(projectFile.getParentFile(), projectFile.getName() + ".files");
    }

    /**
     * Saves the project, including textures and glTF model.  If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @param projectFile The file path for the project.
     * @param finishedCallback
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public void saveProject(File projectFile, Runnable finishedCallback) throws IOException, ParserConfigurationException, TransformerException
    {
        ViewSet viewSet = getLoadedViewSet();
        setViewsetDirectories(projectFile, viewSet);

        progressMonitor.setStage(0, "Preparing project...");
        progressMonitor.setFinishingUpText("This shouldn't take long...");

        copyMasks();
        copyModelAndTextures();

        RecentProjects.setMostRecentDirectory(projectFile.getParentFile());

        File filesDirectory = getDefaultSupportingFilesDirectory(projectFile);
        filesDirectory.mkdirs();

        ProjectModel projectModel = Global.state().getProjectModel();

        if (projectFile.getName().toLowerCase(Locale.ROOT).endsWith(".vset"))
        {
            viewSet.setRootDirectory(projectFile.getParentFile());
            viewSet.setSupportingFilesDirectory(filesDirectory);

            this.handler.saveToVSETFile(projectFile);
            loadedProjectFile = projectFile;
            loadedViewSetFile = projectFile;
            projectModel.setProjectName(projectFile.getName());
        }
        else
        {
            viewSet.setRootDirectory(filesDirectory);
            viewSet.setSupportingFilesDirectory(filesDirectory);

            File vsetFile = new File(filesDirectory, projectFile.getName() + ".vset");
            this.handler.saveToVSETFile(vsetFile);
            loadedProjectFile = projectFile;
            loadedViewSetFile = vsetFile;
            projectModel.saveProjectFile(projectFile, vsetFile);
            projectModel.setProjectName(projectFile.getName());
        }

        // Export glTF for Kintsugi 3D Viewer even if not requested
        // TODO: ensure that GLTF texture filenames match default material texture names;
        //  otherwise might not work when launching Kintsugi 3D Viewer from Builder.
        this.handler.saveGLTF(getLoadedViewSet().getSupportingFilesDirectory(), /* defaults */ new ExportSettings());

        // Save textures and basis funtions (will be deferred to graphics thread).
        this.handler.saveAllMaterialFiles(getLoadedViewSet().getSupportingFilesDirectory(), finishedCallback);

        // Add to recent files
        RecentProjects.addToRecentFiles(projectFile.getAbsolutePath());
    }

    /**
     * Saves the project, including textures and glTF model, using the current loaded project filename.
     * If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @param finishedCallback
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public void saveProject(Runnable finishedCallback) throws IOException, ParserConfigurationException, TransformerException
    {
        saveProject(getLoadedProjectFile(), null);
    }

    /**
     * Saves the project, including textures and glTF model, using the current loaded project filename.
     * If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public void saveProject() throws IOException, ParserConfigurationException, TransformerException
    {
        saveProject(getLoadedProjectFile(), null);
    }

    private static void setViewsetDirectories(File projectFile, ViewSet viewSet)
    {
        File filesDirectory = getDefaultSupportingFilesDirectory(projectFile);
        filesDirectory.mkdirs();

        if (Objects.equals(Global.state().getIOModel().getLoadedViewSetFile(), projectFile)) // Saved as a VSET
        {
            viewSet.setRootDirectory(projectFile.getParentFile());
        }
        else // Saved as a Kintsugi 3D project
        {
            viewSet.setRootDirectory(filesDirectory);
        }

        // Requires root directory to be previously assigned
        viewSet.setSupportingFilesDirectory(filesDirectory);
    }

    public DoubleUnaryOperator getLuminanceEncodingFunction()
    {
        return this.handler.getLuminanceEncodingFunction();
    }

    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.handler.setTonemapping(linearLuminanceValues, encodedLuminanceValues);
    }

    public void clearTonemapping()
    {
        this.handler.clearTonemapping();
    }

    public void requestLightIntensityCalibration()
    {
        this.handler.requestLightIntensityCalibration();
    }

    public void applyLightCalibration()
    {
        this.handler.applyLightCalibration();
    }

    public void closeProject()
    {
        unload();

        ProjectModel projectModel = Global.state().getProjectModel();
        projectModel.setProjectOpen(false);
        projectModel.clearProjectName();
    }

    private void unload()
    {
        this.unload(() -> {});
    }

    private void unload(Runnable onUnloadComplete)
    {
        loadedViewSetFile = null;
        loadedProjectFile = null;
        this.handler.unload(onUnloadComplete);
    }

    public boolean hasLoadedRenderable()
    {
        return this.handler != null && this.handler.isRenderableLoaded();
    }

    public boolean hasValidHandler()
    {
        return this.handler != null;
    }

    /**
     * Checks if this has a valid project instance loaded.  Otherwise, throws an IllegalStateException.
     * @return This model if it has a valid project instance.
     */
    public IOModel validateRenderable()
    {
        if (!hasLoadedRenderable())
        {
            throw new IllegalStateException("No project loaded.");
        }

        return this;
    }

    /**
     * Checks for whether srcFile is null before copying into destDir.
     *
     * @param srcFile
     * @param destDir
     */
    private static void copyFileSafe(File srcFile, File destDir)
    {
        if (srcFile != null)
        {
            try
            {
                File destFile = new File(destDir, srcFile.getName());
                Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                LOG.error("Failed to copy {} to {}", srcFile.getName(), destDir.getPath());
            }
        }
    }

    /**
     * Copies masks to an appropriate supporting files directory and changes the masks directory accordingly.
     * If the masks were previously stored in a ZIP file, they will be unzipped to the new masks directory.
     * Masks will be validated (see validateMasks()) as a result of this operation, possibly changing the recorded mask file name
     * based on the mask files that are actually found (or eliminating masks if missing).
     */
    private void copyMasks()
    {
        ViewSet viewSet = getLoadedViewSet();
        File masksSrcDir = viewSet.getMasksDirectory();
        if (masksSrcDir == null)
        {
            return;
        }

        // Grab reference first just in case for thread synchronization.
        File masksDestParentDir = viewSet.getSupportingFilesDirectory();

        File masksDestinationDir;
        if (masksDestParentDir != null)
        {
            masksDestinationDir = new File(masksDestParentDir, "masks");
        }
        else
        {
            masksDestinationDir = new File(
                ApplicationFolders.getExtensionDirectory().resolve("kintsugi3d.builder.masks").toFile(),
                viewSet.getUUID().toString());
        }

        masksDestinationDir.mkdirs();

        // Unzip masks if needed
        if (masksSrcDir.toString().endsWith(".zip"))
        {
            LOG.info("Unzipping masks folder...");
            try
            {
                // Just unzip everything for efficiency; could clean up any unused files (i.e. non-masks) but probably not necessary
                UnzipHelper.unzipToDirectory(masksSrcDir, masksDestinationDir, null);

                // Use the destination directory as the masks directory for validating (and thereafter)
                viewSet.setMasksDirectory(masksDestinationDir);

                // Make sure the masks are there after unzipping (might change the mask filenames stored)
                viewSet.validateMasks();
            }
            catch (IOException e)
            {
                LOG.error("Failed to unzip masks.", e);
            }
        }
        else
        {
            // Validate masks first to make sure we're copying the right files (might change the mask filenames stored)
            viewSet.validateMasks();

            // Copy the list for thread safety without blocking while it copies all the files.
            Iterable<View> viewsCopy = viewSet.getViews();

            // Copy the files that were actually found
            for (View view : viewsCopy)
            {
                File maskSrcFile = view.getMaskFile();
                copyFileSafe(maskSrcFile, masksDestinationDir);
            }

            // Use the destination directory as the masks directory to use from now on.
            viewSet.setMasksDirectory(masksDestinationDir);
        }
    }

    /**
     * Copies model and textures to an appropriate supporting files directory and changes the model directory accordingly.
     * If the model and textures were previously stored in a ZIP file, they will be unzipped to the new model directory.
     */
    private void copyModelAndTextures()
    {
        ViewSet viewSet = getLoadedViewSet();

        // Grab reference first just in case for thread synchronization.
        File modelDestParentDir = viewSet.getSupportingFilesDirectory();

        File modelDestDir;
        if (modelDestParentDir != null)
        {
            modelDestDir = new File(modelDestParentDir, "model");
        }
        else
        {
            modelDestDir = new File(
                ApplicationFolders.getExtensionDirectory().resolve("kintsugi3d.builder.model").toFile(),
                viewSet.getUUID().toString());
        }

        modelDestDir.mkdirs();

        // Grab copy for thread synchronization, just in case.
        File geometryFile = viewSet.getGeometryFile();

        // Unzip model and textures if needed
        if (geometryFile.toString().endsWith(".zip"))
        {
            // Assuming a Metashape-zipped PLY model called "mesh.ply".
            LOG.info("Unzipping model folder...");
            try
            {
                // Just unzip everything for efficiency; could clean up any unused files but probably not necessary
                UnzipHelper.unzipToDirectory(geometryFile, modelDestDir, null);

                viewSet.setGeometryFile(new File(modelDestDir, "mesh.ply"));

                // Copy textures from the destination directory.
                copyTextures(modelDestDir);
            }
            catch (IOException e)
            {
                LOG.error("Failed to unzip model / textures.", e);
            }
        }
        else
        {
            copyFileSafe(geometryFile, modelDestDir);

            for (var resource : viewSet.getResourceMap().entrySet())
            {
                if (resource.getKey().startsWith("texture."))
                {
                    copyFileSafe(resource.getValue(), modelDestDir);
                }
            }

            // Use the destination directory as the model directory to use from now on.
            viewSet.setGeometryFile(new File(modelDestDir, geometryFile.getName()));

            // Copy textures from the "original" file directory
            copyTextures(geometryFile.getParentFile());
        }
    }

    private void copyTextures(File textureDirectory)
    {
        if (textureDirectory != null)
        {
            File metashapeModelXMLFile = new File(textureDirectory, "doc.xml");
            if (metashapeModelXMLFile.exists())
            {
                copyTexturesFromSupplier(new MetashapeTextures(metashapeModelXMLFile));
            }
            else
            {
                VertexGeometry geometry = handler.getLoadedGeometry();

                if (geometry != null)
                {
                    ImportedMaterial material = geometry.getMaterial();

                    if (material != null)
                    {
                        copyTexturesFromSupplier(new OBJMaterialTextures(material, textureDirectory));
                    }
                }
            }
        }
    }

    private void copyTexturesFromSupplier(TextureSupplier textureSupplier)
    {
        for (Entry<String, File> entry : textureSupplier.getTextures().entrySet())
        {
            copyTexture(entry.getValue(), entry.getKey());
        }
    }

    private void copyTexture(File originalFile, String texName)
    {
        if (originalFile != null && originalFile.exists())
        {
            File outTex = TextureResources.getTextureFile(texName, getLoadedViewSet().getSupportingFilesDirectory());
            try
            {
                // Force conversion to PNG.
                ImageHelper.read(originalFile).save("PNG", outTex);
            }
            catch (IOException e)
            {
                LOG.error("Could not copy {} texture from {}.", texName, originalFile);
            }
        }
    }
}
