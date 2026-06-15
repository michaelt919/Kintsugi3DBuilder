/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.gltf.usdz;

import de.javagl.jgltf.impl.v2.TextureInfo;
import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.io.gltf.MaterialExporter;
import kintsugi3d.builder.io.gltf.StandardTextureExport;
import kintsugi3d.builder.rendering.StandardShaderComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class USDZExporter extends MaterialExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(USDZExporter.class);
    private static final String SCRIPT_LOCATION = "/home/nathan/Documents/Kintsugi3DBuilder/python/";
    private File outputPath;

    @StandardTextureExport(StandardTexture.NORMAL_MAP)
    public void normal(TextureInfo normal)
    {

    }

    @StandardTextureExport(StandardTexture.DIFFUSE_COLOR)
    public void diffuse(TextureInfo diffuse)
    {

    }

    @StandardTextureExport(StandardTexture.SPECULAR_COLOR)
    public void specular(TextureInfo specular)
    {

    }

    @StandardTextureExport(StandardTexture.ROUGHNESS)
    public void roughness(TextureInfo roughness)
    {

    }

    @Override
    protected void postExport()
    {
        // base_name texture_extension normal diffuse specular roughness
        try
        {
            // may need to set up a python install toolchain
            String pythonExecutable = SCRIPT_LOCATION + ".venv/bin/python";
            String script = SCRIPT_LOCATION + "scripts/converter.py";
            String normal = getTextureFilename(StandardTexture.NORMAL_MAP.texName, "PNG");
            String diffuse = getTextureFilename(StandardTexture.DIFFUSE_COLOR.texName, "PNG");
            String specular = getTextureFilename(StandardTexture.SPECULAR_COLOR.texName, "PNG");
            String roughness = getTextureFilename(StandardTexture.ROUGHNESS.texName, "PNG");

            ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable,
                script,
                getFilename(),
                getTextureFileFormat(),
                normal,
                diffuse,
                specular,
                roughness
            );

            pb.directory(outputPath);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
            {
                reader.lines().forEachOrdered(LOG::info);
            }

            if (process.waitFor() != 0)
            {
                throw new Exception();
            }
        }
        catch (Exception e)
        {
            LOG.error("Could not export USDZ file.");
        }
    }

    // Grab the output directory after the super call
    @Override
    public void saveTextures(File outputDirectory)
    {
        super.saveTextures(outputDirectory);

        outputPath = outputDirectory;
    }
}
