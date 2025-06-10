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

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.builder.export.specular.SpecularFitSerializer;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.DoubleVector3;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BasisResources<ContextType extends Context<ContextType>> implements Resource, ContextBound<ContextType>
{
    private final ContextType context;
    private final Texture2D<ContextType> basisMaps;
    private final UniformBuffer<ContextType> diffuseUniformBuffer;
    private final int basisCount;
    private final int basisResolution;

    private List<DoubleVector3> diffuseAlbedos;
    private MaterialBasis materialBasis;

    public BasisResources(ContextType context, int basisCount, int basisResolution)
    {
        this.context = context;
        this.basisCount = basisCount;
        this.basisResolution = basisResolution;

        this.basisMaps = context.getTextureFactory().build1DColorTextureArray(
                basisResolution + 1, basisCount)
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

    public MaterialBasis getSpecularBasis()
    {
        return materialBasis;
    }

    public int getBasisCount()
    {
        return basisCount;
    }

    public int getBasisResolution()
    {
        return basisResolution;
    }

    public void updateFromSolution(SpecularDecomposition solution)
    {
        this.materialBasis = solution.getMaterialBasis();
        this.diffuseAlbedos = solution.getDiffuseAlbedos();

        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
        NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3,
            basisCount * (basisResolution + 1));
        NativeVectorBuffer diffuseNativeBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, basisCount);

        for (int b = 0; b < basisCount; b++)
        {
            // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
            for (int m = 0; m <= basisResolution; m++)
            {
                // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                basisMapBuffer.set(m + (basisResolution + 1) * b, 0, this.materialBasis.evaluateSpecularRed(b, m));
                basisMapBuffer.set(m + (basisResolution + 1) * b, 1, this.materialBasis.evaluateSpecularGreen(b, m));
                basisMapBuffer.set(m + (basisResolution + 1) * b, 2, this.materialBasis.evaluateSpecularBlue(b, m));
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
    public static <ContextType extends Context<ContextType>> BasisResources<ContextType> loadFromPriorSolution(
        ContextType context, File priorSolutionDirectory) throws IOException
    {
        MaterialBasis basis = SpecularFitSerializer.deserializeBasisFunctions(priorSolutionDirectory);

        if (basis != null)
        {
            NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();

            // Set up basis function buffer
            NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3,
                basis.getMaterialCount() * (basis.getSpecularResolution() + 1));

            for (int b = 0; b < basis.getMaterialCount(); b++)
            {
                // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
                for (int m = 0; m <= basis.getSpecularResolution(); m++)
                {
                    // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                    basisMapBuffer.set(m + (basis.getSpecularResolution() + 1) * b, 0, basis.evaluateSpecularRed(b, m));
                    basisMapBuffer.set(m + (basis.getSpecularResolution() + 1) * b, 1, basis.evaluateSpecularGreen(b, m));
                    basisMapBuffer.set(m + (basis.getSpecularResolution() + 1) * b, 2, basis.evaluateSpecularBlue(b, m));
                }
            }

            // Diffuse albedos
            NativeVectorBuffer diffuseNativeBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, basis.getMaterialCount());
            for (int b = 0; b < basis.getMaterialCount(); b++)
            {
                // Store each channel of the diffuse albedo in the local buffer.
                diffuseNativeBuffer.set(b, 0, basis.getDiffuseColor(b).x);
                diffuseNativeBuffer.set(b, 1, basis.getDiffuseColor(b).y);
                diffuseNativeBuffer.set(b, 2, basis.getDiffuseColor(b).z);
                diffuseNativeBuffer.set(b, 3, 1.0f);
            }

            BasisResources<ContextType> resources = new BasisResources<>(context, basis.getMaterialCount(), basis.getSpecularResolution());
            resources.materialBasis = basis;

            // Send the basis functions to the GPU.
            resources.basisMaps.load(basisMapBuffer);

            // Send the diffuse albedos to the GPU.
            resources.diffuseUniformBuffer.setData(diffuseNativeBuffer);

            return resources;
        }
        else
        {
            return null;
        }
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