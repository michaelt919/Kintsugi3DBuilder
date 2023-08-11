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

package kintsugi3d.builder.export.specular;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.util.ImageLodResizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpecularFitTextureRescaler
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitTextureRescaler.class);
    private final ExportSettings settings;

    public SpecularFitTextureRescaler(ExportSettings settings)
    {
        this.settings = settings;
    }

    public void rescaleAll(File outputDirectory, int basisCount)
    {
        List<String> files = new ArrayList<>(List.of(new String[]{"albedo.png", "diffuse.png", "specular.png", "orm.png", "normal.png"}));

        if (settings.isCombineWeights())
        {
            for (int i = 0; i < (basisCount + 3) / 4; i++)
            {
                files.add(SpecularFitSerializer.getCombinedWeightFilename(i));
            }
        }
        else
        {
            for (int i = 0; i < basisCount; i++)
            {
                files.add(SpecularFitSerializer.getWeightFileName(i));
            }
        }

        for (String file : files)
        {
            try
            {
                generateLodsFor(new File(outputDirectory, file));
            }
            catch (IOException e)
            {
                log.error("Error generating LODs for file '{}':", file, e);
            }
        }
    }

    public void generateLodsFor(File file) throws IOException
    {
        ImageLodResizer.generateLods(file, settings.getMinimumTextureResolution());
    }

}
