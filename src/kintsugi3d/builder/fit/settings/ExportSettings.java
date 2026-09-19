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

package kintsugi3d.builder.fit.settings;

import kintsugi3d.builder.io.gltf.MaterialExporter;
import kintsugi3d.builder.io.gltf.MaterialExporterFactory;
import kintsugi3d.builder.io.gltf.ModelExporter;
import kintsugi3d.builder.io.gltf.kintsugi3dviewer.Kintsugi3DViewerExporterFactory;
import kintsugi3d.builder.resources.project.specular.ReadonlyTextureResources;

import java.util.Locale;

public class ExportSettings implements ReadonlyExportSettings
{
    private boolean combineWeights = true;
    private MaterialExporterFactory exporterFactory = Kintsugi3DViewerExporterFactory.getInstance();
    private boolean exportTextures = false;
    private boolean appendModelNameToTextures = false;
    private String textureFormat = "PNG";
    private boolean generateLowResTextures = false;
    private int minimumTextureResolution = 128;

    private boolean openViewerOnceComplete = false;

    @Override
    public boolean shouldSaveTextures()
    {
        return exportTextures;
    }

    public void setShouldSaveTextures(boolean exportTextures)
    {
        this.exportTextures = exportTextures;
    }

    @Override
    public boolean shouldAppendModelNameToTextures()
    {
        return appendModelNameToTextures;
    }

    public void setShouldAppendModelNameToTextures(boolean appendModelNameToTextures)
    {
        this.appendModelNameToTextures = appendModelNameToTextures;
    }

    @Override
    public String getTextureFormat()
    {
        return textureFormat;
    }

    public void setTextureFormat(String textureFormat)
    {
        this.textureFormat = textureFormat;
    }

    @Override
    public boolean shouldCombineWeights()
    {
        return combineWeights;
    }

    public void setShouldCombineWeights(boolean combineWeights)
    {
        this.combineWeights = combineWeights;
    }

    @Override
    public boolean shouldGenerateLowResTextures()
    {
        return generateLowResTextures;
    }

    public void setShouldGenerateLowResTextures(boolean generateLowResTextures)
    {
        this.generateLowResTextures = generateLowResTextures;
    }

    @Override
    public int getMinimumTextureResolution()
    {
        return minimumTextureResolution;
    }

    public void setMinimumTextureResolution(int minimumTextureResolution)
    {
        this.minimumTextureResolution = minimumTextureResolution;
    }

    @Override
    public boolean shouldOpenViewerOnceComplete()
    {
        return openViewerOnceComplete;
    }

    public void setShouldOpenViewerOnceComplete(boolean openViewerOnceComplete)
    {
        this.openViewerOnceComplete = openViewerOnceComplete;
    }

    @Override
    public MaterialExporterFactory getExporterFactory()
    {
        return exporterFactory;
    }

    public void setExporterFactory(MaterialExporterFactory exporterFactory)
    {
        this.exporterFactory = exporterFactory;
    }

    @Override
    public void applyToExporter(ModelExporter exporter, ReadonlyTextureResources<? extends kintsugi3d.gl.core.Context<?>> textureResources, String filename)
    {
        MaterialExporter materialExporter = exporterFactory.getExporter(textureResources);
        exporter.setMaterialExporter(materialExporter);
        materialExporter.setFilename(filename);
        materialExporter.setCombineWeights(combineWeights);
        materialExporter.setTextureFileFormat(textureFormat);

        if (generateLowResTextures)
        {
            materialExporter.setMinLODSize(minimumTextureResolution);
        }

        if (appendModelNameToTextures)
        {
            String baseName = filename;
            if (baseName.toLowerCase(Locale.ROOT).endsWith(".gltf"))
            {
                baseName = baseName.substring(0, baseName.length() - 5);
            }
            else if (baseName.toLowerCase(Locale.ROOT).endsWith(".glb"))
            {
                baseName = baseName.substring(0, baseName.length() - 4);
            }

            materialExporter.setTextureFilePrefix(String.format("%s_", baseName));
        }
    }
}
