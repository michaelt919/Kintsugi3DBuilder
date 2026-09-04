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
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.texture.StandardTexture;
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
import java.util.Comparator;
import java.util.List;

public abstract class USDZExporter extends MaterialExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(USDZExporter.class);
    private static final Path BIN_LOCATION = ApplicationFolders.getAdditionalBinDirectory();

    private File outputPath;
    private File tempPath;

    protected final File getTempPath()
    {
        return tempPath;
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    @StandardTextureExport(StandardTexture.NORMAL_MAP)
    public void normal(TextureInfo normal)
    {
    }

    @SuppressWarnings("NoopMethodInAbstractClass")
    @StandardTextureExport(StandardTexture.ROUGHNESS)
    public void roughness(TextureInfo roughness)
    {
    }

    protected abstract void addCommands(List<String> commandList);

    @Override
    protected void postExport()
    {
        // Command list for the ProcessBuilder
        List<String> command = new ArrayList<>(8);

        // Get the wildcarded application name for the exporter
        String glob;
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
                throw new RuntimeException("OS environment not supported.");
        }

        // Attempt to realize path for the exporter application
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BIN_LOCATION, glob))
        {
            Path path = stream.iterator().next();

            if (!Files.exists(path))
            {
                throw new IOException("Could not find USDZ exporter binary.");
            }

            command.add(path.toString());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage());
        }

        command.add("--model");
        command.add(getFilename());
        command.add("--format");
        command.add(getTextureFileFormat());
        command.add("--normal");
        command.add(new File(tempPath, getTextureFilename(StandardTexture.NORMAL_MAP.details.name, getTextureFileFormat())).getPath());
        command.add("--roughness");
        command.add(new File(tempPath, getTextureFilename(StandardTexture.ROUGHNESS.details.name, getTextureFileFormat())).getPath());
        // Add inheritance specified commands
        addCommands(command);

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
                throw new RuntimeException("Passed files don't match requirements.");
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Couldn't open the USDZ exporter.");
        }
        catch (InterruptedException e)
        {
            LOG.error("Process was interrupted.");
            return;
        }

        // Cleanup temp files
        try
        {
            Files.walk(tempPath.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        catch (IOException e)
        {
            LOG.error("Couldn't cleanup temp files");
        }
    }

    // Grab the output directory and redirect the save to a temporary directory
    @Override
    public void saveTextures(File outputDirectory)
    {
        outputPath = outputDirectory;
        tempPath = new File(Global.state().getIOModel().getLoadedViewSet().getSupportingFilesDirectory(), "temp");
        super.saveTextures(tempPath);
    }
}