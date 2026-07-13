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
    private File outputPath;
    private boolean cycles = false;

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
        try
        {
            // Command list for the ProcessBuilder
            List<String> command = new ArrayList<>();

            switch (OperatingSystem.getCurrentOS())
            {
                case WINDOWS:
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("C:\\Program Files\\Blender Foundation\\"), "Blender*"))
                    {
                        String path = "";
                        for (Path entry : stream)
                        {
                            path = entry.toAbsolutePath().toString();
                            break;
                        }

                        if (path.isEmpty())
                        {
                            throw new IllegalStateException("Could not find Blender.");
                        }

                        command.add(path + "\\blender.exe");
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
                    throw new IllegalStateException("OS environment not supported.");
            }

            command.add("--python");
            command.add(SCRIPT_LOCATION + "/open-blender.py");
            command.add("--model");
            command.add(outputPath.getAbsolutePath() + "/" + getFilename());
            command.add("--normal");
            command.add(outputPath.getAbsolutePath() + "/" + getTextureFilename(StandardTexture.NORMAL_MAP.texName, getTextureFileFormat()));
            command.add("--diffuse");
            command.add(outputPath.getAbsolutePath() + "/" + getTextureFilename(StandardTexture.DIFFUSE_COLOR.texName, getTextureFileFormat()));
            command.add("--specular");
            command.add(outputPath.getAbsolutePath() + "/" + getTextureFilename(StandardTexture.SPECULAR_COLOR.texName, getTextureFileFormat()));
            command.add("--roughness");
            command.add(outputPath.getAbsolutePath() + "/" + getTextureFilename(StandardTexture.ROUGHNESS.texName, getTextureFileFormat()));
            if (cycles)
            {
                command.add("--use-cycles");
            }

            ProcessBuilder pb = new ProcessBuilder(command);

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
        catch (IllegalStateException | InterruptedException | IOException e)
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
