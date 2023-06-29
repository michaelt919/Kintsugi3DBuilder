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

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.ImageCache;

@SuppressWarnings("PublicField")
public class BasisResources<ContextType extends Context<ContextType>> implements Resource, CloneCroppable<BasisResources<ContextType>>
{
    public final ContextType context;

    public final Texture3D<ContextType> weightMaps;
    public final Texture2D<ContextType> weightMask;
    public final Texture2D<ContextType> basisMaps;
    public final UniformBuffer<ContextType> diffuseUniformBuffer;

    /**
     * When creating "resources" for a block the basis maps and diffuse uniform buffer are reused from the parent resources.
     * Thus they shouldn't be closed when the block is closed.
     */
    private final boolean nonWeightResourcesOwned;

    private final int width;
    private final int height;
    private final SpecularBasisSettings specularBasisSettings;

    public BasisResources(ContextType context, int width, int height, SpecularBasisSettings specularBasisSettings)
    {
        this(context,
            // Weight maps:
            context.getTextureFactory().build2DColorTextureArray(width, height, specularBasisSettings.getBasisCount())
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture(),
            // Weight mask:
            context.getTextureFactory().build2DColorTexture(width, height)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture(),
            // Basis maps:
            context.getTextureFactory().build1DColorTextureArray(
                    specularBasisSettings.getMicrofacetDistributionResolution() + 1, specularBasisSettings.getBasisCount())
                .setInternalFormat(ColorFormat.RGB32F)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture(),
            // Uniform buffer:
            context.createUniformBuffer(),
            true, specularBasisSettings);
    }

    private BasisResources(ContextType context, Texture3D<ContextType> weightMaps, Texture2D<ContextType> weightMask,
        Texture2D<ContextType> basisMaps, UniformBuffer<ContextType> diffuseUniformBuffer, boolean nonWeightResourcesOwned,
        SpecularBasisSettings specularBasisSettings)
    {
        this.context = context;
        this.width = weightMaps.getWidth();
        this.height = weightMaps.getHeight();
        this.specularBasisSettings = specularBasisSettings;
        this.weightMaps = weightMaps;
        this.weightMask = weightMask;
        this.basisMaps = basisMaps;
        this.diffuseUniformBuffer = diffuseUniformBuffer;

        this.nonWeightResourcesOwned = nonWeightResourcesOwned;

        this.weightMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None, TextureWrapMode.None);

        if (nonWeightResourcesOwned)
        {
            this.basisMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None);
        }
    }

    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return specularBasisSettings;
    }

    public void updateFromSolution(SpecularDecomposition solution)
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
        NativeVectorBuffer weightMaskBuffer = factory.createEmpty(NativeDataType.FLOAT, 1,
            width * height);
        NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3,
            specularBasisSettings.getBasisCount() * (specularBasisSettings.getMicrofacetDistributionResolution() + 1));
        NativeVectorBuffer diffuseNativeBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, specularBasisSettings.getBasisCount());

        // Load weight mask first.
        for (int p = 0; p < width * height; p++)
        {
            weightMaskBuffer.set(p, 0, solution.areWeightsValid(p) ? 1.0 : 0.0);
        }

        weightMask.load(weightMaskBuffer);

        for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
        {
            // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
            for (int p = 0; p < width * height; p++)
            {
                weightMaskBuffer.set(p, 0, solution.getWeights(p).get(b));
            }

            // Immediately load the weight map so that we can reuse the local memory buffer.
            weightMaps.loadLayer(b, weightMaskBuffer);

            // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
            for (int m = 0; m <= specularBasisSettings.getMicrofacetDistributionResolution(); m++)
            {
                // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 0, solution.getSpecularRed().get(m, b));
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 1, solution.getSpecularGreen().get(m, b));
                basisMapBuffer.set(m + (specularBasisSettings.getMicrofacetDistributionResolution() + 1) * b, 2, solution.getSpecularBlue().get(m, b));
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
     * Loads weight maps and basis functions from a prior solution.
     * Does not load diffuse basis colors, so a diffuse map should instead be optimized to cover diffuse.
     * @param priorSolutionDirectory The directory from which to load a prior solution.
     * @throws IOException If a part of the solution cannot be loaded form file.
     */
    public void loadFromPriorSolution(File priorSolutionDirectory) throws IOException
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();

        // Fill trivial weight mask.
        NativeVectorBuffer weightMaskBuffer = factory.createEmpty(NativeDataType.FLOAT, 1,
            width * height);
        for (int p = 0; p < width * height; p++)
        {
            weightMaskBuffer.set(p, 0, 1.0);
        }
        weightMask.load(weightMaskBuffer);

        // Set up basis function buffer
        NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3,
            specularBasisSettings.getBasisCount() * (specularBasisSettings.getMicrofacetDistributionResolution() + 1));

        SpecularBasis basis = SpecularFitSerializer.deserializeBasisFunctions(priorSolutionDirectory);

        for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
        {
            // Load weight maps
            weightMaps.loadLayer(b, new File(priorSolutionDirectory, SpecularFitSerializer.getWeightFileName(b)), true);

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
        program.setTexture("weightMaps", weightMaps);
        program.setTexture("weightMask", weightMask);
        program.setUniformBuffer("DiffuseColors", diffuseUniformBuffer);
    }
    @Override
    public BasisResources<ContextType> crop(int x, int y, int cropWidth, int cropHeight)
    {
        return new BasisResources<>(context, weightMaps.crop(x, y, cropWidth, cropHeight), weightMask.crop(x, y, cropWidth, cropHeight),
            basisMaps, diffuseUniformBuffer, false, specularBasisSettings);
    }

    @Override
    public void close()
    {
        weightMaps.close();
        weightMask.close();

        if (nonWeightResourcesOwned)
        {
            basisMaps.close();
            diffuseUniformBuffer.close();
        }
    }
}
