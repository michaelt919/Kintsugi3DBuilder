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

package kintsugi3d.builder.io.specular;

import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.gl.util.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpecularFitLODGenerator
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitLODGenerator.class);
    private final ExportSettings settings;

    public SpecularFitLODGenerator(ExportSettings settings)
    {
        this.settings = settings;
    }

    public void generateAllLODs(File outputDirectory, String prefix, String format, int basisCount)
    {
        List<String> files = Stream.of("albedo", "diffuse", "specular", "orm", "normal")
            .map(base -> String.format("%s%s.%s", prefix, base, format.toLowerCase(Locale.ROOT)))
            .collect(Collectors.toList());

        if (settings.shouldCombineWeights())
        {
            for (int i = 0; i < (basisCount + 3) / 4; i++)
            {
                files.add(SpecularFitSerializer.getCombinedWeightFilename(i, format));
            }
        }
        else
        {
            for (int i = 0; i < basisCount; i++)
            {
                files.add(SpecularFitSerializer.getWeightFileName(i, format));
            }
        }

        for (String file : files)
        {
            try
            {
                generateLODsFor(new File(outputDirectory, file));
            }
            catch (IOException e)
            {
                LOG.error("Error generating LODs for file '{}':", file, e);
            }
        }
    }

    public void generateLODsFor(File file) throws IOException
    {
        int minSize = settings.getMinimumTextureResolution();

        String filename = file.getName();
        String extension = "";
        int i = filename.lastIndexOf('.'); //Strip file extension
        if (i > 0)
        {
            extension = filename.substring(i);
            filename = filename.substring(0, i);
        }

        ImageHelper imageHelper = ImageHelper.read(file);

        for (int size = imageHelper.getBufferedImage().getHeight() / 2; size >= minSize; size /= 2)
        {
            imageHelper.saveAtResolution(new File(file.getParent(), filename + "-" + size + extension), size);
        }
    }

}
