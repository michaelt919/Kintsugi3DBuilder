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

package kintsugi3d.builder.fit.roughness;

import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected RoughnessOptimizationBase(BasisResources<ContextType> basisResources)
        throws IOException
    {
        // Fit specular parameters from weighted basis functions
        specularRoughnessFitProgram = basisResources.getContext().getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/specularRoughnessFitNew.frag"))
                .define("BASIS_COUNT", basisResources.getBasisCount())
                .define("BASIS_RESOLUTION", basisResources.getBasisResolution())
                .createProgram();

        // Create basic rectangle vertex buffer
        rect = basisResources.getContext().createRectangle();
        specularRoughnessFitDrawable = basisResources.getContext().createDrawable(specularRoughnessFitProgram);
        specularRoughnessFitDrawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);

        // Set up shader program
        specularRoughnessFitDrawable.addVertexBuffer("position", rect);
        specularRoughnessFitProgram.setUniform("fittingGamma", 1.0f);
        basisResources.useWithShaderProgram(specularRoughnessFitProgram);

    }

    @Override
    public final void setInputWeights(BasisWeightResources<ContextType> weightResources)
    {
        weightResources.useWithShaderProgram(specularRoughnessFitProgram);
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
        return getFramebuffer() == null ? null : getFramebuffer().getColorAttachmentTexture(0);
    }

    @Override
    public Texture2D<ContextType> getRoughnessTexture()
    {
        return getFramebuffer() == null ? null : getFramebuffer().getColorAttachmentTexture(1);
    }

    @Override
    public void execute(float gamma)
    {
        specularRoughnessFitProgram.setUniform("gamma", gamma);
        specularRoughnessFitProgram.setUniform("gammaInv", 1.0f / gamma);

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
            getFramebuffer().getTextureReaderForColorAttachment(0)
                .saveToFile("PNG", new File(outputDirectory, "specular.png"));
            getFramebuffer().getTextureReaderForColorAttachment(1)
                .saveToFile("PNG", new File(outputDirectory, "roughness.png"));
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
        specularRoughnessFitDrawable.close();
        rect.close();
    }
}
