/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.ibrelight.rendering.resources;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.SampledLuminanceEncoding;

public final class LuminanceMapResources<ContextType extends Context<ContextType>> implements Resource
{
    private final ContextType context;

    private Texture1D<ContextType> luminanceMap;

    private Texture1D<ContextType> inverseLuminanceMap;

    public static <ContextType extends Context<ContextType>> LuminanceMapResources<ContextType> createNull(ContextType context)
    {
        return new LuminanceMapResources<>(context);
    }

    private LuminanceMapResources(ContextType context)
    {
        this.context = context;
        this.luminanceMap = null;
        this.inverseLuminanceMap = null;
    }

    public static <ContextType extends Context<ContextType>> LuminanceMapResources<ContextType> createFromEncoding(
            ContextType context, SampledLuminanceEncoding luminanceEncoding)
    {
        return new LuminanceMapResources<>(context, luminanceEncoding);
    }

    private LuminanceMapResources(ContextType context, SampledLuminanceEncoding luminanceEncoding)
    {
        this.context = context;
        this.luminanceMap = luminanceEncoding.createLuminanceMap(context);
        this.inverseLuminanceMap = luminanceEncoding.createInverseLuminanceMap(context);
    }

    /**
     * A 1D texture defining how encoded RGB values should be converted to linear luminance.
     */
    public Texture1D<ContextType> getLuminanceMap()
    {
        return luminanceMap;
    }

    /**
     * A 1D texture defining how encoded RGB values should be converted to linear luminance.
     */
    public Texture1D<ContextType> getInverseLuminanceMap()
    {
        return inverseLuminanceMap;
    }

    public void update(SampledLuminanceEncoding luminanceEncoding)
    {
        if (luminanceMap != null)
        {
            luminanceMap.close();
            luminanceMap = null;
        }

        if (inverseLuminanceMap != null)
        {
            inverseLuminanceMap.close();
            inverseLuminanceMap = null;
        }

        if (luminanceEncoding != null)
        {
            luminanceMap = luminanceEncoding.createLuminanceMap(context);
            inverseLuminanceMap = luminanceEncoding.createInverseLuminanceMap(context);
        }
    }

    public void setupShaderProgram(Program<ContextType> program)
    {
        if (this.luminanceMap == null)
        {
            program.setTexture("luminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
        }
        else
        {
            program.setTexture("luminanceMap", this.luminanceMap);
        }

        if (this.inverseLuminanceMap == null)
        {
            program.setTexture("inverseLuminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
        }
        else
        {
            program.setTexture("inverseLuminanceMap", this.inverseLuminanceMap);
        }
    }

    @Override
    public void close()
    {
        if (luminanceMap != null)
        {
            luminanceMap.close();
        }

        if (inverseLuminanceMap != null)
        {
            inverseLuminanceMap.close();
        }
    }
}
