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

import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;

import java.io.File;
import java.io.IOException;

public class BasisResources<ContextType extends Context<ContextType>> implements Resource, ContextBound<ContextType>
{
    private final ContextType context;
    private final Texture2D<ContextType> basisMaps;
    private final UniformBuffer<ContextType> diffuseUniformBuffer;
    private final SpecularBasisSettings specularBasisSettings;

    public BasisResources(ContextType context, SpecularBasisSettings specularBasisSettings)
    {
        this.context = context;
        this.specularBasisSettings = specularBasisSettings;

        this.basisMaps = context.getTextureFactory().build1DColorTextureArray(
                specularBasisSettings.getMicrofacetDistributionResolution() + 1, specularBasisSettings.getBasisCount())
            .setInternalFormat(ColorFormat.RGB32F)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(false)
            .createTexture();
        basisMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None);

        this.diffuseUniformBuffer = context.createUniformBuffer();
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return specularBasisSettings;
    }

    public void updateFromSolution(SpecularDecomposition solution)
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
        NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3,
            specularBasisSettings.getBasisCount() * (specularBasisSettings.getMicrofacetDistributionResolution() + 1));
        NativeVectorBuffer diffuseNativeBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, specularBasisSettings.getBasisCount());

        for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
        {
            // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
            for (int m = 0; m <= specularBasisSettings.getMicrofacetDistributionResolution(); m++)
            {
                // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 0, solution.evaluateRed(b, m));
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 1, solution.evaluateGreen(b, m));
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 2, solution.evaluateBlue(b, m));
            }

            // Store each channel of the diffuse albedo in the local buffer.
            diffuseNativeBuffer.set(b, 0, solution.getDiffuseAlbedo(b).x);
            diffuseNativeBuffer.set(b, 1, solution.getDiffuseAlbedo(b).y);
            diffuseNativeBuffer.set(b, 2, solution.getDiffuseAlbedo(b).z);
            diffuseNativeBuffer.set(b, 3, 1.0f);
        }

        // Send the basis functions to the GPU.
        basisMaps.load(basisMapBuffer);

        // Send the diffuse albedos to the GPU.
        diffuseUniformBuffer.setData(diffuseNativeBuffer);
    }

    /**
     * Loads basis functions from a prior solution.
     * Does not load diffuse basis colors, so a diffuse map should instead be optimized to cover diffuse.
     * @param priorSolutionDirectory The directory from which to load a prior solution.
     * @throws IOException If a part of the solution cannot be loaded form file.
     */
    public void loadFromPriorSolution(File priorSolutionDirectory) throws IOException
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();

        // Set up basis function buffer
        NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3,
            specularBasisSettings.getBasisCount() * (specularBasisSettings.getMicrofacetDistributionResolution() + 1));

        SpecularBasis basis = SpecularFitSerializer.deserializeBasisFunctions(priorSolutionDirectory);

        for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
        {
            // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
            for (int m = 0; m <= specularBasisSettings.getMicrofacetDistributionResolution(); m++)
            {
                // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 0, basis.evaluateRed(b, m));
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 1, basis.evaluateGreen(b, m));
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 2, basis.evaluateBlue(b, m));
            }
        }

        // Send the basis functions to the GPU.
        basisMaps.load(basisMapBuffer);

        // Skip diffuse basis colors -- we'll optimize a diffuse map separately, so it shouldn't matter if they're all black.
    }

    public void useWithShaderProgram(Program<ContextType> program)
    {
        program.setTexture("basisFunctions", basisMaps);
        program.setUniformBuffer("DiffuseColors", diffuseUniformBuffer);
    }

    @Override
    public void close()
    {
        basisMaps.close();
        diffuseUniformBuffer.close();
    }
}