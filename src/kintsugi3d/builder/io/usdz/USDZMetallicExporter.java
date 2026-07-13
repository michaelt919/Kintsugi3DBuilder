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

package kintsugi3d.builder.io.usdz;

import de.javagl.jgltf.impl.v2.TextureInfo;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.app.OperatingSystem;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class USDZMetallicExporter extends MaterialExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(USDZMetallicExporter.class);
    private static final Path SCRIPT_LOCATION = ApplicationFolders.getAdditionalBinDirectory();
    private File outputPath;

    @StandardTextureExport(StandardTexture.NORMAL_MAP)
    public void normal(TextureInfo normal)
    {

    }

    @StandardTextureExport(StandardTexture.ALBEDO)
    public void albedo(TextureInfo diffuse)
    {

    }

    @StandardTextureExport(StandardTexture.ORM)
    public void orm(TextureInfo specular)
    {

    }

    @Override
    protected void postExport()
    {
        // glb texture_extension normal diffuse specular roughness
        try
        {
            String normal = getTextureFilename(StandardTexture.NORMAL_MAP.texName, getTextureFileFormat());
            String albedo = getTextureFilename(StandardTexture.ALBEDO.texName, getTextureFileFormat());
            String orm = getTextureFilename(StandardTexture.ORM.texName, getTextureFileFormat());

            String executable = "";
            String glob;

            // Get the wildcarded application name for the exporter
            switch (OperatingSystem.getCurrentOS())
            {
                case WINDOWS:
                    glob = "usdz-exporter*windows.exe";
                    break;

                case MACOS:
                    glob = "usdz-exporter*macos";
                    break;

                case UNIX:
                    glob = "usdz-exporter*linux";
                    break;

                default:
                    throw new IllegalStateException("OS environment not supported.");
            }

            // Attempt to realize path for the exporter application
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(SCRIPT_LOCATION, glob))
            {
                for (Path entry : stream)
                {
                    executable = entry.toAbsolutePath().toString();
                    break;
                }

                if (executable.isEmpty())
                {
                    throw new IllegalStateException("Could not find USDZ exporter binary.");
                }
            }

            // Define a new process to start the exporter
            ProcessBuilder pb = new ProcessBuilder(
                executable,
                "--metallic",
                "--model", getFilename(),
                "--format", getTextureFileFormat(),
                "--normal", normal,
                "--albedo", albedo,
                "--orm", orm
            );

            // Change the working directory of the exporter to the output path
            pb.directory(outputPath);
            Process process = pb.start();

            // Initialize a logger for the exporter log output
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
            {
                reader.lines().forEachOrdered(LOG::info);
            }

            // If the exporter didn't exit with a 0, an error occurred
            if (process.waitFor() != 0)
            {
                throw new IllegalArgumentException("Passed files don't match requirements.");
            }
        }
        catch (IllegalArgumentException |
               IllegalStateException |
               IOException |
               InterruptedException e)
        {
            LOG.error(e.getMessage());
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
