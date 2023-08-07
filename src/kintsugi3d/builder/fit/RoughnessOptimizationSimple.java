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

package kintsugi3d.builder.fit;

import java.io.FileNotFoundException;

import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.core.*;
import kintsugi3d.builder.core.TextureFitSettings;

public class RoughnessOptimizationSimple<ContextType extends Context<ContextType>> extends RoughnessOptimizationBase<ContextType>
{
    private final FramebufferObject<ContextType> specularTexFramebuffer;

    public RoughnessOptimizationSimple(BasisResources<ContextType> basisResources,
        BasisWeightResources<ContextType> weightResources, TextureFitSettings settings)
        throws FileNotFoundException
    {
        super(basisResources, weightResources, settings.gamma);

        // Framebuffer for fitting and storing the specular parameter estimates (specular Fresnel color and roughness)
        specularTexFramebuffer = basisResources.getContext().buildFramebufferObject(
                settings.width, settings.height)
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
            .createFramebufferObject();
    }

    @Override
    protected FramebufferObject<ContextType> getFramebuffer()
    {
        return specularTexFramebuffer;
    }

    @Override
    public void close()
    {
        super.close();
        specularTexFramebuffer.close();
    }
}
