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

import kintsugi3d.builder.core.texture.TextureResolution;
import kintsugi3d.builder.io.specular.WeightImageWriter;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class BasisWeightResources<ContextType extends Context<ContextType>>
    implements ManagedResource, ReadonlyBasisWeightResources<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(BasisWeightResources.class);
    private final ContextType context;

    private Texture3D<ContextType> weightMaps;
    private final Texture2D<ContextType> weightMask;

    private final int width;
    private final int height;

    private final MaterialBasis basis;

    public BasisWeightResources(ContextType context, int width, int height, MaterialBasis basis)
    {
        this(
            // Weight maps (including disabled materials -- enabled materials should precede disabled ones):
            createWeightMaps(context, width, height, basis.getMaterialCount()),
            // Weight mask:
            context.getTextureFactory().build2DColorTexture(width, height)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .createTexture(),
            basis);
    }

    private static <ContextType extends Context<ContextType>>
    Texture3D<ContextType> createWeightMaps(ContextType context, int width, int height, int materialCount)
    {
        return context.getTextureFactory().build2DColorTextureArray(width, height, materialCount)
            .setInternalFormat(ColorFormat.R32F)
            .setLinearFilteringEnabled(true)
            .createTexture();
    }

    /**
     * Takes ownership of the textures passed
     * @param weightMaps
     * @param weightMask
     */
    private BasisWeightResources(Texture3D<ContextType> weightMaps, Texture2D<ContextType> weightMask, MaterialBasis basis)
    {
        this.context = weightMaps.getContext();
        this.width = weightMaps.getWidth();
        this.height = weightMaps.getHeight();
        this.basis = basis;
        this.weightMaps = weightMaps;
        this.weightMask = weightMask;
        this.weightMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None, TextureWrapMode.None);
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public int getMaterialCount()
    {
        return basis.getMaterialCount();
    }

    @Override
    public int getActiveMaterialCount()
    {
        return basis.getEnabledMaterialCount();
    }

    @Override
    public Texture3D<ContextType> getWeightMaps()
    {
        return weightMaps;
    }

    @Override
    public Texture2D<ContextType> getWeightMask()
    {
        return weightMask;
    }

    public void updateFromSolution(SpecularDecomposition solution)
    {
        NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
        NativeVectorBuffer buffer = factory.createEmpty(NativeDataType.FLOAT, 1,
            width * height);

        // Load weight mask first.
        for (int p = 0; p < width * height; p++)
        {
            buffer.set(p, 0, solution.areWeightsValid(p) ? 1.0 : 0.0);
        }

        weightMask.load(buffer);

        int count = solution.getMaterialBasis().getEnabledMaterialCount();
        for (int b = 0; b < count; b++)
        {
            // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
            for (int p = 0; p < width * height; p++)
            {
                buffer.set(p, 0, solution.getWeights(p).get(b));
            }

            // Immediately load the weight map so that we can reuse the local memory buffer.
            weightMaps.loadLayer(b, buffer);
        }

        // Now fill any unused layers (i.e. disabled materials) with zeros.
        for (int p = 0; p < width * height; p++)
        {
            buffer.set(p, 0, 0.0);
        }

        for (int b = count; b < weightMaps.getDepth(); b++)
        {
            weightMaps.loadLayer(b, buffer);
        }
    }

    public static File findWeightmap(File solutionDirectory, String materialName) throws FileNotFoundException
    {
        File file = new File(solutionDirectory, getUnpackedWeightMapFilename(materialName));

        try
        {
            return ImageFinder.getInstance().findImageFile(file);
        }
        catch (FileNotFoundException e)
        {
            try
            {
                // Fallback for loading older projects that may not have had zero-padding in basisFunctions.csv
                int index = Integer.parseInt(materialName);
                File altFile = ImageFinder.getInstance().tryFindImageFile(
                    new File(solutionDirectory, getUnpackedWeightMapFilename(String.format("%02d", index))));

                if (altFile != null)
                {
                    return altFile;
                }
                else
                {
                    // re-throw the original exception if the fallback failed.
                    throw e;
                }
            }
            catch (NumberFormatException ignored)
            {
                // re-throw the original exception if the fallback failed.
                throw e;
            }
        }
    }

    /**
     * Loads weight maps and basis functions from a prior solution.
     * Does not load diffuse basis colors, so a diffuse map should instead be optimized to cover diffuse.
     * @param context
     * @param priorSolutionDirectory The directory from which to load a prior solution.
     * @param basis
     * @throws IOException If a part of the solution cannot be loaded form file.
     */
    public static <ContextType extends Context<ContextType>> BasisWeightResources<ContextType> loadFromPriorSolution(
        ContextType context, File priorSolutionDirectory, MaterialBasis basis)
            throws IOException
    {
        String name = basis.getMaterials().stream()
            .filter(BasisMaterialInfo::isEnabled)
            .findAny().orElseThrow()
            .getName();

        IntVector2 dimensions = ImageHelper.dimensionsOf(findWeightmap(priorSolutionDirectory, name));

        int width = dimensions.x;
        int height = dimensions.y;

        BasisWeightResources<ContextType> resources = new BasisWeightResources<>(context, width, height, basis);

        // Fill trivial weight mask.
        NativeVectorBuffer weightMaskBuffer = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1,
            width * height);
        for (int p = 0; p < width * height; p++)
        {
            weightMaskBuffer.set(p, 0, 1.0);
        }
        resources.weightMask.load(weightMaskBuffer);

        for (BasisMaterialInfo material : basis.getMaterials())
        {
            File file = findWeightmap(priorSolutionDirectory, material.getName());
            try
            {
                // Load weight maps
                resources.weightMaps.loadLayer(material.getGPUIndex(), file, true);
            }
            catch (IOException e)
            {
                LOG.error("Error loading weight map: {}", file, e);
            }
        }

        return resources;
    }

    @Override
    public void savePacked(int weightsPerImage, String format, File outputDirectory, IntFunction<String> filenameGenerator)
    {
        // Save the packed weight maps for used by external viewer
        try
        {
            // Packed basis materials are assumed to exclude disabled materials; viewers do not expect disabled materials
            save(weightsPerImage, getActiveMaterialCount(), format, outputDirectory, filenameGenerator);
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving unpacked weight maps.", e);
        }
    }

    @Override
    public void saveUnpacked(String format, File outputDirectory, String filenamePrefix)
    {
        List<? extends BasisMaterialInfo> materialList = basis.getIndexableMaterialList();

        // Save the unpacked weight maps for reloading the project in the future
        try
        {
            // Explicitly use the listed name for unpacked weightmaps to ensure that they align with the names
            // listed in basisMaterials.csv, and include any disabled materials present in the weight map array.
            save(1, Math.min(getMaterialCount(), weightMaps.getDepth()), format, outputDirectory,
                i -> getUnpackedWeightMapFilename(materialList.get(i).getName(), format, filenamePrefix));
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving unpacked weight maps.", e);
        }
    }

    private void save(int weightsPerImage, int materialCount, String format, File outputDirectory, IntFunction<String> filenameGenerator)
        throws IOException
    {
        // Save the unpacked weight maps for reloading the project in the future
        try (WeightImageWriter<ContextType> weightImageWriter =
                 new WeightImageWriter<>(getContext(), TextureResolution.of(weightMaps), weightsPerImage))
        {
            int fileCount = (basis.getEnabledMaterialCount() + weightsPerImage - 1) / weightsPerImage;
            weightImageWriter.saveImages(this, materialCount, format, outputDirectory,
                IntStream.range(0, fileCount)
                    .mapToObj(filenameGenerator)
                    .toArray(String[]::new));
        }
    }

    @Override
    public void useWithShaderProgram(Program<ContextType> program)
    {
        program.setTexture("weightMaps", weightMaps);
        program.setTexture("weightMask", weightMask);
    }

    public void deleteWeightMap(int mapIndex)
    {
        int texArrayDepth = weightMaps.getDepth();
        Texture3D<ContextType> newWeightMaps = createWeightMaps(context, width, height, texArrayDepth - 1);

        // Copy layers before the one removed.v
        newWeightMaps.blitCropped(weightMaps, 0, 0, 0, width, height, mapIndex);

        // Copy layers after the one removed.
        newWeightMaps.blitCropped(0, 0, mapIndex,
            weightMaps, 0, 0, mapIndex + 1, width, height, texArrayDepth - mapIndex - 1);

        // Replace
        weightMaps.close();
        weightMaps = newWeightMaps;
    }

    @Override
    public BasisWeightResources<ContextType> crop(int x, int y, int cropWidth, int cropHeight)
    {
        return new BasisWeightResources<>(
            weightMaps.crop(x, y, cropWidth, cropHeight), weightMask.crop(x, y, cropWidth, cropHeight), basis);
    }

    /**
     * Replaces weightmap by loading the file with a default, expected filename in a specified directory.
     * @param materialName
     * @param parentDirectory
     */
    public void replaceWeightMapWithDefaultFile(String materialName, File parentDirectory) throws IOException
    {
        replaceWeightMapWithSpecificFile(materialName, findWeightmap(parentDirectory, materialName));
    }

    /**
     * Replaces weightmap by loading a specified file.
     * @param materialName
     * @param newTextureFile
     */
    public void replaceWeightMapWithSpecificFile(String materialName, File newTextureFile) throws IOException
    {
        weightMaps.loadLayer(basis.getMaterial(materialName).getGPUIndex(), newTextureFile, true);
    }

    @Override
    public void close()
    {
        weightMaps.close();
        weightMask.close();
    }

    public static String getUnpackedWeightMapFilename(String materialName, String format, String filenamePrefix)
    {
        return TextureResources.getTextureFilename(getUnpackedWeightMapName(materialName), format, filenamePrefix);
    }

    public static String getUnpackedWeightMapFilename(String materialName, String format)
    {
        return getUnpackedWeightMapFilename(materialName, format, "");
    }

    public static String getUnpackedWeightMapFilename(String materialName)
    {
        return getUnpackedWeightMapFilename(materialName, "PNG");
    }

    public static String getUnpackedWeightMapName(String materialName)
    {
        return String.format("weights%s", materialName);
    }
}
