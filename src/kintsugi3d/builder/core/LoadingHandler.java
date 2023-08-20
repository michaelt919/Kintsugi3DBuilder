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

public interface LoadingHandler 
{
    boolean isInstanceLoaded();
    ViewSet getLoadedViewSet();
    void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions);
    void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File imageDirectory,
        String primaryViewName, ReadonlyLoadOptionsModel loadOptions);

    void requestFragmentShader(File shaderFile);

    Optional<AbstractImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException;
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    void saveToVSETFile(File vsetFile) throws IOException;

    void unload();

    void setLoadingMonitor(LoadingMonitor loadingMonitor);

    DoubleUnaryOperator getLuminanceEncodingFunction();
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    void applyLightCalibration();
}
