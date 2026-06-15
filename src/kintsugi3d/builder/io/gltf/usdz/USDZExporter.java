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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;


public class USDZExporter extends MaterialExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(USDZExporter.class);
    private static final File PYTHON_LOCATION_MACOS = Path.of("python", "bin", "python3").toFile();
    private static final File SCRIPT_LOCATION = new File("python-scripts");
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
            String pythonExecutable = PYTHON_LOCATION_MACOS.getAbsolutePath();
            String script = new File(SCRIPT_LOCATION, "converter.py").getAbsolutePath();
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

            Thread standardOutput = new Thread(() ->
            {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
                {
                    reader.lines().forEachOrdered(LOG::info);
                }
                catch (IOException e)
                {
                    LOG.error("Error reading from standard output.", e);
                }
            });

            standardOutput.start();

            Thread standardError = new Thread(() ->
            {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)))
                {
                    reader.lines().forEachOrdered(LOG::error);
                }
                catch (IOException e)
                {
                    LOG.error("Error reading from standard error.", e);
                }
            });

            standardError.start();

            // Wait to finish receiving process output.
            standardOutput.join();
            standardError.join();

            int exitCode = process.waitFor();

            if (exitCode != 0)
            {
                LOG.error("Could not export USDZ file.  Error code: {}", exitCode);
            }
        }
        catch (IOException|InterruptedException|RuntimeException e)
        {
            LOG.error("Could not export USDZ file.", e);
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
