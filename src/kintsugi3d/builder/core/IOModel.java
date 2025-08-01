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
import kintsugi3d.util.EncodableColorImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public class IOModel
{
    private static class AggregateProgressMonitor implements ProgressMonitor
    {
        private final Collection<ProgressMonitor> subMonitors = new ArrayList<>();

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
    public void requestFragmentShader(File shaderFile)
    {
        this.handler.requestFragmentShader(shaderFile);
    }

    public void requestFragmentShader(File shaderFile, Map<String, Optional<Object>> extraDefines)
    {
        this.handler.requestFragmentShader(shaderFile, extraDefines);
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

    public void saveAllMaterialFiles(File materialDirectory, Runnable finishedCallback)
    {
        this.handler.saveAllMaterialFiles(materialDirectory, finishedCallback);
    }

    public void saveEssentialMaterialFiles(File materialDirectory, Runnable finishedCallback)
    {
        this.handler.saveEssentialMaterialFiles(materialDirectory, finishedCallback);
    }

    public void saveGlTF(File outputDirectory, ExportSettings settings)
    {
        this.handler.saveGlTF(outputDirectory, settings);
    }

    public void saveGlTF(File outputDirectory)
    {
        this.handler.saveGlTF(outputDirectory, new ExportSettings() /* defaults */);
    }


    public DoubleUnaryOperator getLuminanceEncodingFunction()
    {
        return this.handler.getLuminanceEncodingFunction();
    }

    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.handler.setTonemapping(linearLuminanceValues, encodedLuminanceValues);
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
}
