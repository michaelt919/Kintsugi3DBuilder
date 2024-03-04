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

package kintsugi3d.builder.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.util.AbstractImage;

public class IOModel
{
    private static class AggregateLoadingMonitor implements LoadingMonitor
    {
        private final Collection<LoadingMonitor> subMonitors = new ArrayList<>();

        void addSubMonitor(LoadingMonitor monitor)
        {
            subMonitors.add(monitor);
        }

        @Override
        public void startLoading()
        {
            for (LoadingMonitor monitor : subMonitors)
            {
                monitor.startLoading();
            }
        }

        @Override
        public void setMaximum(double maximum)
        {
            for (LoadingMonitor monitor : subMonitors)
            {
                monitor.setMaximum(maximum);
            }
        }

        @Override
        public void setProgress(double progress)
        {
            for (LoadingMonitor monitor : subMonitors)
            {
                monitor.setProgress(progress);
            }
        }

        @Override
        public void loadingComplete()
        {
            for (LoadingMonitor monitor : subMonitors)
            {
                monitor.loadingComplete();
            }
        }

        @Override
        public void loadingFailed(Exception e)
        {
            for (LoadingMonitor monitor : subMonitors)
            {
                monitor.loadingFailed(e);
            }
        }

        @Override
        public void loadingWarning(Exception e)
        {
            for (LoadingMonitor monitor : subMonitors)
            {
                monitor.loadingWarning(e);
            }
        }
    }

    private IOHandler handler;
    private final AggregateLoadingMonitor loadingMonitor = new AggregateLoadingMonitor();
    private ReadonlyLoadOptionsModel loadOptionsModel;

    public LoadingMonitor getLoadingMonitor()
    {
        return loadingMonitor;
    }

    public void setLoadingHandler(IOHandler handler)
    {
        this.handler = handler;
        this.handler.setLoadingMonitor(loadingMonitor);
    }

    public void addLoadingMonitor(LoadingMonitor monitor)
    {
        this.loadingMonitor.addSubMonitor(monitor);
    }

    public void setLoadOptionsModel(ReadonlyLoadOptionsModel loadOptionsModel)
    {
        this.loadOptionsModel = loadOptionsModel;
    }

    public boolean isInstanceLoaded()
    {
        return this.handler != null && this.handler.isInstanceLoaded();
    }

    public void addViewSetLoadCallback(Consumer<ViewSet> callback)
    {
        this.handler.addViewSetLoadCallback(callback);
    }

    public ViewSet getLoadedViewSet()
    {
        return this.handler.getLoadedViewSet();
    }

    /**
     * Uses parent of VSET file as supporting files directory, by default
     * @param id
     * @param vsetFile
     */
    public void loadFromVSETFile(String id, File vsetFile)
    {
        this.handler.loadFromVSETFile(id, vsetFile, vsetFile.getParentFile(), loadOptionsModel);
    }

    public void loadFromVSETFile(String id, File vsetFile, File supportingFilesDirectory)
    {
        this.handler.loadFromVSETFile(id, vsetFile, supportingFilesDirectory, loadOptionsModel);
    }

    public void loadFromAgisoftFiles(String id, File xmlFile, File meshFile, File undistortedImageDirectory, String primaryViewName)
    {
        this.handler.loadFromAgisoftXMLFile(id, xmlFile, meshFile, undistortedImageDirectory, primaryViewName, loadOptionsModel);
    }

    public void requestFragmentShader(File shaderFile)
    {
        this.handler.requestFragmentShader(shaderFile);
    }

    public Optional<AbstractImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
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

    public void saveMaterialFiles(File materialDirectory, Runnable finishedCallback)
    {
        this.handler.saveMaterialFiles(materialDirectory, finishedCallback);
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
