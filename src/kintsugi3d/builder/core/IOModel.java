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

package kintsugi3d.builder.core;

import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.javafx.core.RecentProjects;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.util.EncodableColorImage;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public class IOModel
{
    private static class AggregateProgressMonitor implements ProgressMonitor
    {
        private final Collection<ProgressMonitor> subMonitors = new ArrayList<>(8);

        void addSubMonitor(ProgressMonitor monitor)
        {
            subMonitors.add(monitor);
        }

        @Override
        public void allowUserCancellation() throws UserCancellationException
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.allowUserCancellation();
            }
        }

        @Override
        public void cancelComplete(UserCancellationException e)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.cancelComplete(e);
            }
        }

        @Override
        public void start()
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.start();
            }
        }

        @Override
        public void setProcessName(String processName) {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.setProcessName(processName);
            }
        }

        @Override
        public void setStageCount(int count)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.setStageCount(count);
            }
        }

        @Override
        public void setStage(int stage, String message)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.setStage(stage, message);
            }
        }

        @Override
        public void advanceStage(String message)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.advanceStage(message);
            }
        }

        @Override
        public void setMaxProgress(double maxProgress)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.setMaxProgress(maxProgress);
            }
        }

        @Override
        public void setProgress(double progress, String message)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.setProgress(progress, message);
            }
        }

        @Override
        public void complete()
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.complete();
            }
        }

        @Override
        public void fail(Throwable e)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.fail(e);
            }
        }

        @Override
        public void warn(Throwable e)
        {
            for (ProgressMonitor monitor : subMonitors)
            {
                monitor.warn(e);
            }
        }

        @Override
        public boolean isConflictingProcess() {
            boolean processing = false;
            for (ProgressMonitor monitor : subMonitors)
            {
                if(monitor.isConflictingProcess()){
                    processing = true;
                }
            }

            return processing;
        }
    }

    private IOHandler handler;
    private final AggregateProgressMonitor progressMonitor = new AggregateProgressMonitor();
    private ReadonlyLoadOptionsModel imageLoadOptionsModel;

    public ProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
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

    public void addViewSetLoadCallback(Consumer<ViewSet> callback)
    {
        this.handler.addViewSetLoadCallback(callback);
    }

    public ViewSet getLoadedViewSet()
    {
        return this.handler.getLoadedViewSet();
    }

    public File getLoadedProjectFile()
    {
        return this.handler.getLoadedProjectFile();
    }

    public void setLoadedProjectFile(File loadedProjectFile)
    {
        this.handler.setLoadedProjectFile(loadedProjectFile);
    }

    /**
     * Uses parent of VSET file as supporting files directory, by default
     * @param id
     * @param vsetFile
     */
    public void loadFromVSETFile(String id, File vsetFile)
    {
        this.handler.loadFromVSETFile(id, vsetFile, vsetFile.getParentFile(), imageLoadOptionsModel);
    }

    public void loadFromVSETFile(String id, File vsetFile, File supportingFilesDirectory)
    {
        this.handler.loadFromVSETFile(id, vsetFile, supportingFilesDirectory, imageLoadOptionsModel);
    }

    public void loadFromLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions)
    {
        this.handler.loadFromLooseFiles(id, xmlFile, viewSetLoadOptions, imageLoadOptionsModel);
    }

    public void hotSwapLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions)
    {
        viewSetLoadOptions.uuid = getLoadedViewSet() != null ? getLoadedViewSet().getUUID() : null;
        this.handler.loadFromLooseFiles(id, xmlFile, viewSetLoadOptions, imageLoadOptionsModel);
    }

    public void loadFromMetashapeModel(MetashapeModel model)
    {
        this.handler.loadFromMetashapeModel(model, imageLoadOptionsModel);
    }

    public Optional<EncodableColorImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        return this.handler.loadEnvironmentMap(environmentMapFile);
    }

    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        this.handler.loadBackplate(backplateFile);
    }

    public void saveToVSETFile(File vsetFile) throws IOException
    {
        this.handler.saveToVSETFile(vsetFile);
    }

    public static File getDefaultSupportingFilesDirectory(File projectFile)
    {
        return new File(projectFile.getParentFile(), projectFile.getName() + ".files");
    }

    /**
     * Saves the project.  If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @param projectFile The file path for the project.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public File saveProject(File projectFile) throws IOException, ParserConfigurationException, TransformerException
    {
        RecentProjects.setMostRecentDirectory(projectFile.getParentFile());

        File filesDirectory = getDefaultSupportingFilesDirectory(projectFile);
        filesDirectory.mkdirs();

        ViewSet viewSet = getLoadedViewSet();
        ProjectModel projectModel = Global.state().getProjectModel();

        if (projectFile.getName().toLowerCase(Locale.ROOT).endsWith(".vset"))
        {
            viewSet.setRootDirectory(projectFile.getParentFile());
            viewSet.setSupportingFilesDirectory(filesDirectory);

            saveToVSETFile(projectFile);
            setLoadedProjectFile(projectFile);
            projectModel.setProjectName(projectFile.getName());

            return projectFile;
        }
        else
        {
            viewSet.setRootDirectory(filesDirectory);
            viewSet.setSupportingFilesDirectory(filesDirectory);

            File vsetFile = new File(filesDirectory, projectFile.getName() + ".vset");
            saveToVSETFile(vsetFile);
            setLoadedProjectFile(projectFile);
            projectModel.saveProjectFile(projectFile, vsetFile);
            projectModel.setProjectName(projectFile.getName());

            return vsetFile;
        }
    }

    /**
     * Saves the project using the current loaded project filename.
     * If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public File saveProject() throws IOException, ParserConfigurationException, TransformerException
    {
        return saveProject(getLoadedProjectFile());
    }

    public void saveAllMaterialFiles(File materialDirectory, Runnable finishedCallback)
    {
        this.handler.saveAllMaterialFiles(materialDirectory, finishedCallback);
    }

    public void saveAllMaterialFiles(Runnable finishedCallback)
    {
        this.handler.saveAllMaterialFiles(getLoadedViewSet().getSupportingFilesDirectory(), finishedCallback);
    }

    public void saveAllMaterialFiles()
    {
        this.handler.saveAllMaterialFiles(getLoadedViewSet().getSupportingFilesDirectory(), null);
    }

    public void saveGLTF(File outputDirectory, ExportSettings settings)
    {
        this.handler.saveGLTF(outputDirectory, settings);
    }

    public void saveGLTF(File outputDirectory)
    {
        saveGLTF(outputDirectory, new ExportSettings() /* defaults */);
    }

    public void saveGLTF()
    {
        saveGLTF(getLoadedViewSet().getSupportingFilesDirectory());
    }

    /**
     * Saves the project, including textures and glTF model.  If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @param projectFile The file path for the project.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public File saveAll(File projectFile, Runnable finishedCallback) throws IOException, ParserConfigurationException, TransformerException
    {
        File vsetFile = saveProject(projectFile);

        // Export glTF for Kintsugi 3D Viewer even if not requested
        // TODO: ensure that GLTF texture filenames match default material texture names;
        //  otherwise might not work when launching Kintsugi 3D Viewer from Builder.
        saveGLTF();

        // Save textures and basis funtions (will be deferred to graphics thread).
        saveAllMaterialFiles(finishedCallback);

        return vsetFile;
    }

    /**
     * Saves the project, including textures and glTF model, using the current loaded project filename.
     * If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public void saveAll() throws IOException, ParserConfigurationException, TransformerException
    {
        saveAll(getLoadedProjectFile(), null);
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

    public void unload()
    {
        this.handler.unload();
    }

    public boolean hasValidHandler()
    {
        return this.handler != null && this.handler.isInstanceLoaded();
    }

    /**
     * Checks if this has a valid project instance loaded.  Otherwise, throws an IllegalStateException.
     * @return This model if it has a valid project instance.
     */
    public IOModel validateHandler()
    {
        if (!hasValidHandler())
        {
            throw new IllegalStateException("No project loaded.");
        }

        return this;
    }
}
