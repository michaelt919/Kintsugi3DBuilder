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

package kintsugi3d.builder.fit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class RoughnessOptimizationBase<ContextType extends Context<ContextType>>
        implements RoughnessOptimization<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(RoughnessOptimizationBase.class);

    protected final ProgramObject<ContextType> specularRoughnessFitProgram;
    protected final VertexBuffer<ContextType> rect;
    protected final Drawable<ContextType> specularRoughnessFitDrawable;

    protected RoughnessOptimizationBase(BasisResources<ContextType> basisResources, BasisWeightResources<ContextType> weightResources, float gamma)
        throws FileNotFoundException
    {
        // Fit specular parameters from weighted basis functions
        specularRoughnessFitProgram = basisResources.getContext().getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/specularRoughnessFitNew.frag"))
                .define("BASIS_COUNT", basisResources.getSpecularBasisSettings().getBasisCount())
                .define("MICROFACET_DISTRIBUTION_RESOLUTION", basisResources.getSpecularBasisSettings().getMicrofacetDistributionResolution())
                .createProgram();

        // Create basic rectangle vertex buffer
        rect = basisResources.getContext().createRectangle();
        specularRoughnessFitDrawable = basisResources.getContext().createDrawable(specularRoughnessFitProgram);
        specularRoughnessFitDrawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);

        // Set up shader program
        specularRoughnessFitDrawable.addVertexBuffer("position", rect);
        basisResources.useWithShaderProgram(specularRoughnessFitProgram);
        weightResources.useWithShaderProgram(specularRoughnessFitProgram);
        specularRoughnessFitProgram.setUniform("gamma", gamma);
        specularRoughnessFitProgram.setUniform("fittingGamma", 1.0f);

    }

    @Override
    public void clear()
    {
        // Set initial assumption for reflectivity / roughness
        getFramebuffer().clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
        getFramebuffer().clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    protected abstract FramebufferObject<ContextType> getFramebuffer();

    @Override
    public Texture2D<ContextType> getReflectivityTexture()
    {
        return getFramebuffer().getColorAttachmentTexture(0);
    }

    @Override
    public Texture2D<ContextType> getRoughnessTexture()
    {
        return getFramebuffer().getColorAttachmentTexture(1);
    }

    @Override
    public void execute()
    {
        // Fit specular so that we have a roughness estimate for masking/shadowing.
        getFramebuffer().clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        getFramebuffer().clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
        specularRoughnessFitDrawable.draw(getFramebuffer());
    }

    @Override
    public void saveTextures(File outputDirectory)
    {
        try
        {
            Framebuffer<ContextType> contextTypeFramebuffer1 = getFramebuffer();
            contextTypeFramebuffer1.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(outputDirectory, "specular.png"));
            Framebuffer<ContextType> contextTypeFramebuffer = getFramebuffer();
            contextTypeFramebuffer.getTextureReaderForColorAttachment(1).saveToFile("PNG", new File(outputDirectory, "roughness.png"));
        }
        catch (IOException e)
        {
            log.error("An error occurred while saving textures:", e);
        }
    }

    @Override
    public void close()
    {
        specularRoughnessFitProgram.close();
        rect.close();
    }
}
