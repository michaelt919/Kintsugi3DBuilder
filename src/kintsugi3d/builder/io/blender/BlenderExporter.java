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

package kintsugi3d.builder.io.blender;

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
import java.util.ArrayList;
import java.util.List;

public class BlenderExporter extends MaterialExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(BlenderExporter.class);
    private static final Path SCRIPT_LOCATION = ApplicationFolders.getAdditionalScriptsDirectory();
    private final boolean cycles;
    private File outputPath;

    public BlenderExporter(boolean cycles)
    {
        this.cycles = cycles;
    }

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
        // Command list for the ProcessBuilder
        List<String> command = new ArrayList<>();

        // Find Blender
        switch (OperatingSystem.getCurrentOS())
        {
            case WINDOWS:
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("C:\\Program Files\\Blender Foundation\\"), "Blender*"))
                {
                    Path path = stream.iterator().next();

                    if (!Files.exists(path))
                    {
                        LOG.error("Could not find Blender.");
                        return;
                    }

                    command.add(path.resolve("blender.exe").toString());
                }
                catch (IOException e)
                {
                    LOG.error(e.getMessage());
                    return;
                }
                break;

            case MACOS:
                command.add("/Applications/Blender.app/Contents/MacOS/Blender");
                break;

            case UNIX:
                command.add("bash");
                command.add("-c");
                command.add("blender");
                break;

            default:
                LOG.error("OS environment not supported.");
                return;
        }

        command.add("-b");
        command.add("-P");
        command.add(SCRIPT_LOCATION + "/open-blender.py");
        command.add("--");
        command.add("--model");
        command.add(getFilename());
        command.add("--normal");
        command.add(new File(outputPath,
            getTextureFilename(StandardTexture.NORMAL_MAP.details.name, getTextureFileFormat())).getPath());
        command.add("--diffuse");
        command.add(new File(outputPath,
            getTextureFilename(StandardTexture.DIFFUSE_COLOR.details.name, getTextureFileFormat())).getPath());
        command.add("--specular");
        command.add(new File(outputPath,
            getTextureFilename(StandardTexture.SPECULAR_COLOR.details.name, getTextureFileFormat())).getPath());
        command.add("--roughness");
        command.add(new File(outputPath,
            getTextureFilename(StandardTexture.ROUGHNESS.details.name, getTextureFileFormat())).getPath());

        if (cycles)
        {
            command.add("--use-cycles");
        }

        ProcessBuilder pb = new ProcessBuilder(command);

        // Change the working directory of the exporter to the output path
        pb.directory(outputPath);
        pb.redirectErrorStream(true);
        try
        {
            Process process = pb.start();

            // Initialize a logger for the exporter log output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            reader.lines().forEachOrdered(LOG::info);

            // If the exporter didn't exit with a 0, an error occurred
            if (process.waitFor() != 0)
            {
                LOG.error("Passed files don't match requirements.");
                return;
            }
        }
        catch (IOException e)
        {
            LOG.error("Couldn't open Blender.");
            return;
        }
        catch (InterruptedException e)
        {
            LOG.error("Process was interrupted.");
            return;
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
