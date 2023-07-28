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

import java.io.File;
import java.io.IOException;

import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;

@SuppressWarnings("PublicField")
public class BasisWeightResources<ContextType extends Context<ContextType>>
    implements Resource, ContextBound<ContextType>, Croppable<BasisWeightResources<ContextType>>
{
    private final ContextType context;

    public final Texture3D<ContextType> weightMaps;
    public final Texture2D<ContextType> weightMask;

    private final int width;
    private final int height;
    private final int basisCount;

    public BasisWeightResources(ContextType context, int width, int height, int basisCount)
    {
        this(
            // Weight maps:
            context.getTextureFactory().build2DColorTextureArray(width, height, basisCount)
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture(),
            // Weight mask:
            context.getTextureFactory().build2DColorTexture(width, height)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture());
    }

    /**
     * Takes ownership of the textures passed
     * @param weightMaps
     * @param weightMask
     */
    private BasisWeightResources(Texture3D<ContextType> weightMaps, Texture2D<ContextType> weightMask)
    {
        this.context = weightMaps.getContext();
        this.width = weightMaps.getWidth();
        this.height = weightMaps.getHeight();
        this.basisCount = weightMaps.getDepth();
        this.weightMaps = weightMaps;
        this.weightMask = weightMask;
        this.weightMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None, TextureWrapMode.None);
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    public void updateFromSolution(SpecularDecomposition solution)
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
        NativeVectorBuffer weightMaskBuffer = factory.createEmpty(NativeDataType.FLOAT, 1,
            width * height);

        // Load weight mask first.
        for (int p = 0; p < width * height; p++)
        {
            weightMaskBuffer.set(p, 0, solution.areWeightsValid(p) ? 1.0 : 0.0);
        }

        weightMask.load(weightMaskBuffer);

        for (int b = 0; b < basisCount; b++)
        {
            // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
            for (int p = 0; p < width * height; p++)
            {
                weightMaskBuffer.set(p, 0, solution.getWeights(p).get(b));
            }

            // Immediately load the weight map so that we can reuse the local memory buffer.
            weightMaps.loadLayer(b, weightMaskBuffer);
        }
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

        for (int b = 0; b < basisCount; b++)
        {
            // Load weight maps
            weightMaps.loadLayer(b, new File(priorSolutionDirectory, SpecularFitSerializer.getWeightFileName(b)), true);
        }
    }

    public void useWithShaderProgram(Program<ContextType> program)
    {
        program.setTexture("weightMaps", weightMaps);
        program.setTexture("weightMask", weightMask);
    }

    @Override
    public BasisWeightResources<ContextType> crop(int x, int y, int cropWidth, int cropHeight)
    {
        return new BasisWeightResources<>(weightMaps.crop(x, y, cropWidth, cropHeight), weightMask.crop(x, y, cropWidth, cropHeight));
    }

    @Override
    public void close()
    {
        weightMaps.close();
        weightMask.close();
    }
}
