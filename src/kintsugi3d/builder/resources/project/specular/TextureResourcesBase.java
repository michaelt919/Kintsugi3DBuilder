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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.texture.TextureInfo;
import kintsugi3d.builder.core.texture.WeightmapTextureInfo;
import kintsugi3d.builder.fit.decomposition.*;
import kintsugi3d.builder.util.MappedChange;
import kintsugi3d.builder.util.MappedChange.Type;
import kintsugi3d.builder.util.Observable;
import kintsugi3d.gl.core.Blittable;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.TwoDimensional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class TextureResourcesBase<ContextType extends Context<ContextType>> extends ReadonlyTextureResourcesBase<ContextType>
    implements TextureResources<ContextType>
{
    protected static final Logger LOG = LoggerFactory.getLogger(TextureResourcesBase.class);

    private Observable<MappedChange<String, BasisMaterialInfo>> basisObservable;
    private Observable<MappedChange<String, TextureInfo>> texturesObservable;

    protected abstract MutableBasisResources<ContextType> getMutableBasisResources();

    @Override
    public final ReadonlyBasisResources<ContextType> getBasisResources()
    {
        return getMutableBasisResources();
    }

    protected abstract BasisWeightResources<ContextType> getMutableBasisWeightResources();

    @Override
    public final ReadonlyBasisWeightResources<ContextType> getBasisWeightResources()
    {
        return getMutableBasisWeightResources();
    }

    @Override
    public void deleteBasisMaterial(String materialName)
    {
        MutableBasisResources<ContextType> basisResources = getMutableBasisResources();
        if (basisResources != null)
        {
            BasisMaterialInfo removed = basisResources.deleteBasisMaterial(materialName);

            if (basisObservable != null)
            {
                basisObservable.notifyObservers(new MappedChange<>(Type.REMOVED, materialName, removed));
            }
        }
    }

    @Override
    public void disableBasisMaterials(Collection<String> materialNames)
    {
        MutableBasisResources<ContextType> basisResources = getMutableBasisResources();
        if (basisResources != null)
        {
            Map<String, BasisMaterialInfo> disabled = basisResources.disableBasisMaterials(materialNames);

            if (basisObservable != null)
            {
                basisObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, disabled));
            }
        }
    }

    @Override
    public void enableBasisMaterials(Collection<String> materialNames)
    {
        MutableBasisResources<ContextType> basisResources = getMutableBasisResources();
        if (basisResources != null)
        {
            Map<String, BasisMaterialInfo> enabled = basisResources.enableBasisMaterials(materialNames);

            if (basisObservable != null)
            {
                basisObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, enabled));
            }
        }
    }

    @Override
    public void toggleBasisMaterial(String materialName)
    {
        MutableBasisResources<ContextType> basisResources = getMutableBasisResources();
        if (basisResources != null)
        {
            BasisMaterialInfo toggled = basisResources.toggleBasisMaterial(materialName);

            if (basisObservable != null)
            {
                basisObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, materialName, toggled));
            }
        }
    }

    @Override
    public void replaceTextureWithDefaultFile(TextureInfo key, File parentDirectory) throws IOException
    {
        getTextures().get(key).load(new File(parentDirectory, String.format("%s.png", key.name)), true);

        if (texturesObservable != null)
        {
            texturesObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, key.name, key));
        }
    }

    @Override
    public void replaceTextureWithSpecificFile(TextureInfo key, File newTextureFile) throws IOException
    {
        getTextures().get(key).load(newTextureFile, true);

        // Save project so that the new texture is saved to disk and to ensure saved project consistency.
        Global.io().saveProject(() ->
        {
            // Don't notify observers until after project is saved so that i.e. thumbnails are refreshed.
            if (texturesObservable != null)
            {
                texturesObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, key.name, key));
            }
        });
    }

    @Override
    public void replaceWeightMapWithDefaultFile(BasisMaterialInfo material, File parentDirectory) throws IOException
    {
        getMutableBasisWeightResources().replaceWeightMapWithDefaultFile(material.getName(), parentDirectory);

        if (texturesObservable != null)
        {
            texturesObservable.notifyObservers(new MappedChange<>(Type.MODIFIED,
                BasisWeightResources.getUnpackedWeightMapName(material.getName()), new WeightmapTextureInfo(material)));
        }
    }

    @Override
    public void replaceWeightMapWithSpecificFile(BasisMaterialInfo material, File newTextureFile) throws IOException
    {
        getMutableBasisWeightResources().replaceWeightMapWithSpecificFile(material.getName(), newTextureFile);

        // Save project so that the new weightmap is saved to disk and to ensure saved project consistency.
        Global.io().saveProject(() ->
        {
            // Don't notify observers until after project is saved so that i.e. thumbnails are refreshed.
            if (texturesObservable != null)
            {
                texturesObservable.notifyObservers(new MappedChange<>(Type.MODIFIED,
                    BasisWeightResources.getUnpackedWeightMapName(material.getName()), new WeightmapTextureInfo(material)));
            }
        });
    }

    @Override
    public void replaceAllTexturesWithDefaultFiles(File parentDirectory) throws IOException
    {
        // normal textures
        for (var texture : getTextures().entrySet())
        {
            texture.getValue().load(new File(parentDirectory, String.format("%s.png", texture.getKey().name)), true);
        }

        // weightmaps
        for (BasisMaterialInfo material : getBasisResources().getBasis().getMaterials())
        {
            getMutableBasisWeightResources().replaceWeightMapWithDefaultFile(material.getName(), parentDirectory);
        }

        // notify observers
        if (texturesObservable != null)
        {
            Map<String, TextureInfo> changeMap = new HashMap<>(getTextures().size());

            for (var texture : getTextures().keySet())
            {
                changeMap.put(texture.name, texture);
            }

            for (BasisMaterialInfo material : getBasisResources().getBasis().getMaterials())
            {
                changeMap.put(BasisWeightResources.getUnpackedWeightMapName(material.getName()), new WeightmapTextureInfo(material));
            }

            texturesObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, changeMap));
        }
    }

    private <SourceType extends TwoDimensional> void blitCroppedAndScaledSingle(
        Blittable<SourceType> destTex, int destX, int destY, int destWidth, int destHeight,
        TwoDimensional readSource, SourceType srcTex, int srcX, int srcY, int srcWidth, int srcHeight,
        boolean linearFiltering)
    {
        if (destTex != null && srcTex != null)
        {
            if (destTex.getWidth() == this.getWidth() && destTex.getHeight() == this.getHeight()
                && srcTex.getWidth() == readSource.getWidth() && srcTex.getHeight() == readSource.getHeight())
            {
                // dimensions match, so just do a normal blit
                destTex.blitCroppedAndScaled(destX, destY, destWidth, destHeight,
                    srcTex, srcX, srcY, srcWidth, srcHeight, linearFiltering);
            }
            else
            {
                // dimensions do not match; try to remap rectangles to grab the same relative area in each
                destTex.blitCroppedAndScaled(
                    (int) Math.round((double) destX * destTex.getWidth() / this.getWidth()),
                    (int) Math.round((double) destY * destTex.getHeight() / this.getHeight()),
                    (int) Math.round((double) destWidth * destTex.getWidth() / this.getWidth()),
                    (int) Math.round((double) destHeight * destTex.getHeight() / this.getHeight()),
                    srcTex,
                    (int) Math.round((double) srcX * srcTex.getWidth() / readSource.getWidth()),
                    (int) Math.round((double) srcY * srcTex.getHeight() / readSource.getHeight()),
                    (int) Math.round((double) srcWidth * srcTex.getWidth() / readSource.getWidth()),
                    (int) Math.round((double) srcHeight * srcTex.getHeight() / readSource.getHeight()),
                    linearFiltering);
            }
        }
    }

    /**
     * Copies pixels from part of a blittable to another.  The copying operation will be start at (x, y) within
     * this blittable, and resize if the requested source and destination rectangles are not the same size.
     *
     * @param destX           The left edge of the rectangle to copy into within this blittable.
     * @param destY           The bottom edge of the rectangle to copy into within this blittable.
     * @param destWidth       The width of the rectangle to copy at the destination resolution.
     * @param destHeight      The height of the rectangle to copy at the destination resolution.
     * @param readSource      The blittable source to copy from.
     * @param srcX            The left edge of the rectangle to copy from within the source.
     * @param srcY            The bottom edge of the rectangle to copy from within the source.
     * @param srcWidth        The width of the rectangle to copy at the source resolution.
     * @param srcHeight       The height of the rectangle to copy at the source resolution.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     */
    @Override
    public void blitCroppedAndScaled(
        int destX, int destY, int destWidth, int destHeight,
        ReadonlyTextureResources<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight,
        boolean linearFiltering)
    {
        // Blit each individual texture -- i.e. diffuse, normal, specular reflectivity, specular roughness
        for (var texEntry : getTextures().entrySet())
        {
            if (readSource.getTextures().containsKey(texEntry.getKey())) // both source and destination must contain the texture to blit
            {
                this.blitCroppedAndScaledSingle(texEntry.getValue(), destX, destY, destWidth, destHeight,
                    readSource, readSource.getTexture(texEntry.getKey()), srcX, srcY, srcWidth, srcHeight, linearFiltering);
            }
        }

        // Blit weight maps, weight mask -- handled separately
        if (this.getBasisWeightResources() != null && readSource.getBasisWeightResources() != null)
        {
            blitCroppedAndScaledSingle(this.getMutableBasisWeightResources().getWeightMaps(), destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().getWeightMaps(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
            blitCroppedAndScaledSingle(this.getMutableBasisWeightResources().getWeightMask(), destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().getWeightMask(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        }

        // Notify observers
        if (texturesObservable != null)
        {
            Map<String, TextureInfo> changeMap = new HashMap<>(getTextures().size());

            for (var texture : getTextures().keySet())
            {
                changeMap.put(texture.name, texture);
            }

            texturesObservable.notifyObservers(new MappedChange<>(Type.MODIFIED, changeMap));
        }
    }

    @Override
    public void setBasisObservable(Observable<MappedChange<String, BasisMaterialInfo>> basisObservable)
    {
        this.basisObservable = basisObservable;
    }

    @Override
    public void setTexturesObservable(Observable<MappedChange<String, TextureInfo>> texturesObservable)
    {
        this.texturesObservable = texturesObservable;
    }
}
