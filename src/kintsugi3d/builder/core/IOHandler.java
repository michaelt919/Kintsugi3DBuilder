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
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public interface IOHandler
{
    boolean isInstanceLoaded();
    void addViewSetLoadCallback(Consumer<ViewSet> callback);
    ViewSet getLoadedViewSet();
    File getLoadedProjectFile();
    void setLoadedProjectFile(File loadedProjectFile);
    void loadFromVSETFile(String id, File vsetFile, File supportingFilesDirectory, ReadonlyLoadOptionsModel loadOptions);
    void loadFromLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions, ReadonlyLoadOptionsModel imageLoadOptions);

    void loadFromMetashapeModel(MetashapeModel model, ReadonlyLoadOptionsModel loadOptionsModel);

    void requestFragmentShader(File shaderFile);

    void requestFragmentShader(File shaderFile, Map<String, Optional<Object>> extraDefines);

    Optional<EncodableColorImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException;
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    void saveToVSETFile(File vsetFile) throws IOException;
    void saveAllMaterialFiles(File materialDirectory, Runnable finishedCallback);
    void saveEssentialMaterialFiles(File materialDirectory, Runnable finishedCallback);

    void saveGlTF(File outputDirectory, ExportSettings settings);

    void unload();

    void setProgressMonitor(ProgressMonitor progressMonitor);

    DoubleUnaryOperator getLuminanceEncodingFunction();
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    void applyLightCalibration();
}
