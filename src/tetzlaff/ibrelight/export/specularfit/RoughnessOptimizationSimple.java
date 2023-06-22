/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.FileNotFoundException;

import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.TextureFitSettings;

public class RoughnessOptimizationSimple<ContextType extends Context<ContextType>> extends RoughnessOptimizationBase<ContextType>
{
    private final FramebufferObject<ContextType> specularTexFramebuffer;

    public RoughnessOptimizationSimple(ContextType context, BasisResources<ContextType> resources, TextureFitSettings settings)
        throws FileNotFoundException
    {
        super(context, resources, settings);

        // Framebuffer for fitting and storing the specular parameter estimates (specular Fresnel color and roughness)
        specularTexFramebuffer = context.buildFramebufferObject(settings.width, settings.height)
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
