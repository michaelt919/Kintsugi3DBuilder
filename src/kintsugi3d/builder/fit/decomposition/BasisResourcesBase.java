/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.DoubleVector3;

import java.io.File;
import java.io.IOException;

public abstract class BasisResourcesBase<ContextType extends Context<ContextType>> implements BasisResources<ContextType>
{
    private final int basisResolution;

    private final ContextType context;
    private final UniformBuffer<ContextType> diffuseUniformBuffer;
    private Texture2D<ContextType> basisMaps;

    protected BasisResourcesBase(ContextType context, int materialCount, int basisResolution)
    {
        this.basisResolution = basisResolution;

        this.context = context;
        this.diffuseUniformBuffer = context.createUniformBuffer();
        this.basisMaps = createBasisMaps(context, materialCount, basisResolution);
    }

    private static <ContextType extends Context<ContextType>>
    Texture2D<ContextType> createBasisMaps(ContextType context, int materialCount, int basisResolution)
    {
        Texture2D<ContextType> basisMaps = context.getTextureFactory().build1DColorTextureArray(
                basisResolution + 1, materialCount)
            .setInternalFormat(ColorFormat.RGB32F)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(false)
            .createTexture();
        basisMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None);
        return basisMaps;
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public abstract IndexAssignableMaterialBasis getBasis();

    @Override
    public int getMaterialCount()
    {
        return getBasis().getMaterialCount();
    }

    @Override
    public int getActiveMaterialCount()
    {
        return getBasis().getEnabledMaterialCount();
    }

    @Override
    public int getBasisResolution()
    {
        return basisResolution;
    }

    @Override
    public final void refreshGraphicsResources()
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
        NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3,
            getActiveMaterialCount() * (basisResolution + 1));
        NativeVectorBuffer diffuseNativeBuffer =
            factory.createEmpty(NativeDataType.FLOAT, 4, getActiveMaterialCount());

        // Include disabled materials which should come after all enabled materials.
        int b = 0;
        for (IndexAssignableBasisMaterialInfo material : getBasis().getMaterials())
        {
            // Update the index where the material lives in GPU memory.
            material.setGPUIndex(b);

            // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
            for (int m = 0; m <= basisResolution; m++)
            {
                // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                basisMapBuffer.set(m + (basisResolution + 1) * b, 0, material.evaluateSpecularRed(m));
                basisMapBuffer.set(m + (basisResolution + 1) * b, 1, material.evaluateSpecularGreen(m));
                basisMapBuffer.set(m + (basisResolution + 1) * b, 2, material.evaluateSpecularBlue(m));
            }

            // Store each channel of the diffuse albedo in the local buffer.
            DoubleVector3 diffuseColor = material.getDiffuseColor();
            diffuseNativeBuffer.set(b, 0, diffuseColor.x);
            diffuseNativeBuffer.set(b, 1, diffuseColor.y);
            diffuseNativeBuffer.set(b, 2, diffuseColor.z);
            diffuseNativeBuffer.set(b, 3, 1.0f);

            b++;
        }

        if (getActiveMaterialCount() != basisMaps.getHeight()) // if the number of basis functions has changed, reallocate
        {
            basisMaps.close();
            basisMaps = createBasisMaps(context, getActiveMaterialCount(), basisResolution);
        }

        // Send the basis functions to the GPU.
        basisMaps.load(basisMapBuffer);

        // Send the diffuse albedos to the GPU.
        diffuseUniformBuffer.setData(diffuseNativeBuffer);
    }

    @Override
    public void save(File outputDirectory, String filenameOverride)
    {
        getBasis().save(outputDirectory,
            filenameOverride != null ? filenameOverride : BasisResources.getBasisFunctionsFilename());

        // Save basis image visualization for cards
        try (BasisImageCreator<ContextType> basisImageCreator =
                 new BasisImageCreator<>(getContext(), basisResolution))
        {
            ViewSet viewSet = Global.io().getLoadedViewSet();
            basisImageCreator.createImages(this, viewSet.getThumbnailImageDirectory());
        }
        catch (IOException e)
        {
            Global.state().getProjectModel().error("Error saving basis image thumbnails", e);
        }
    }

    @Override
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
