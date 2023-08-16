/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

public class FilenameConstants
{
    public static final String ALBEDO_BASE_NAME = "albedo";
    public static final String DIFFUSE_BASE_NAME = "diffuse";
    public static final String NORMAL_BASE_NAME = "normal";
    public static final String ORM_BASE_NAME = "orm";
    public static final String SPECULAR_BASE_NAME = "specular";
    public static final String ROUGHNESS_BASE_NAME = "roughness";
    public static final String CONSTANT_BASE_NAME = "constant";
    public static final String QUADRATIC_BASE_NAME = "quadratic";
    public static final String BASIS_BASE_NAME = "basis";
    public static final String WEIGHTS_BASE_NAME = "weights";
    public static final String BASIS_FUNC_CSV_NAME = "basisFunctions.csv";

    public static String getAlbedoFilename(String filetype)
    {
        return String.format("%s.%s", ALBEDO_BASE_NAME, filetype.toLowerCase());
    }

    public static String getDiffuseFilename(String filetype)
    {
        return String.format("%s.%s", DIFFUSE_BASE_NAME, filetype.toLowerCase());
    }

    public static String getNormalFilename(String filetype)
    {
        return String.format("%s.%s", NORMAL_BASE_NAME, filetype.toLowerCase());
    }

    public static String getOrmFilename(String filetype)
    {
        return String.format("%s.%s", ORM_BASE_NAME, filetype.toLowerCase());
    }

    public static String getSpecularFilename(String filetype)
    {
        return String.format("%s.%s", SPECULAR_BASE_NAME, filetype.toLowerCase());
    }

    public static String getRoughnessFilename(String filetype)
    {
        return String.format("%s.%s", ROUGHNESS_BASE_NAME, filetype.toLowerCase());
    }

    public static String getConstantFilename(String filetype)
    {
        return String.format("%s.%s", CONSTANT_BASE_NAME, filetype.toLowerCase());
    }

    public static String getQuadraticFilename(String filetype)
    {
        return String.format("%s.%s", QUADRATIC_BASE_NAME, filetype.toLowerCase());
    }

    public static String getBasisFilename(String filetype, int index)
    {
        return String.format("%s%02d.%s", index, filetype.toLowerCase());
    }

    public static String getWeightsFilename(String filetype, int index, int weightsPerImage)
    {
        if (weightsPerImage <= 1)
        {
            return String.format("%s%02d.%s", WEIGHTS_BASE_NAME, index, filetype.toLowerCase());
        }
        else
        {
            index *= weightsPerImage;
            int last = index + (weightsPerImage - 1);
            return String.format("%s%02d%02d.%s", WEIGHTS_BASE_NAME, index, last, filetype.toLowerCase());
        }
    }

}
