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
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

import kintsugi3d.util.AbstractImage;

public class LoadingModel 
{
    private LoadingHandler handler;
    private LoadingMonitor loadingMonitor;
    private ReadonlyLoadOptionsModel loadOptionsModel;

    public LoadingMonitor getLoadingMonitor()
    {
        return loadingMonitor;
    }

    public void setLoadingHandler(LoadingHandler handler)
    {
        this.handler = handler;

        if (this.loadingMonitor != null)
        {
            this.handler.setLoadingMonitor(loadingMonitor);
        }
    }

    public void setLoadingMonitor(LoadingMonitor monitor)
    {
        this.loadingMonitor = monitor;

        if (this.handler != null)
        {
            this.handler.setLoadingMonitor(monitor);
        }
    }

    public void setLoadOptionsModel(ReadonlyLoadOptionsModel loadOptionsModel)
    {
        this.loadOptionsModel = loadOptionsModel;
    }

    public void loadFromVSETFile(String id, File vsetFile)
    {
        this.handler.loadFromVSETFile(id, vsetFile, loadOptionsModel);
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
