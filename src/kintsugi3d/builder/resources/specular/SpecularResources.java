/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.resources.specular;

import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.*;

public interface SpecularResources<ContextType extends Context<ContextType>>
    extends AutoCloseable, ContextBound<ContextType>, Blittable<SpecularResources<ContextType>>
{
    default Texture2D<ContextType> getDiffuseMap()
    {
        return null;
    }

    Texture2D<ContextType> getNormalMap();

    default Texture2D<ContextType> getConstantMap()
    {
        return null;
    }

//    Texture2D<ContextType> getQuadraticMap();

    Texture2D<ContextType> getSpecularReflectivityMap();
    Texture2D<ContextType> getSpecularRoughnessMap();

    /**
     * Width and height are based on albedo map so this should never return null, unless getWidth() and getHeight() are overridden.
     * @return
     */
    default Texture2D<ContextType> getAlbedoMap()
    {
        // Diffuse map can substitute for albedo map if there's no albedo map
        return getDiffuseMap();
    }

    default Texture2D<ContextType> getORMMap()
    {
        return null;
    }

    default Texture2D<ContextType> getOcclusionMap()
    {
        // Occlusion would be in red channel so ORM can be used in place of occlusion if no explicit occlusion map
        return getORMMap();
    }

    default BasisResources<ContextType> getBasisResources()
    {
        return null;
    }

    default BasisWeightResources<ContextType> getBasisWeightResources()
    {
        return null;
    }

    @Override
    void close(); // no exception

    private <SourceType extends Blittable<?>> void blitCroppedAndScaledSingle(
        Blittable<SourceType> destTex, int destX, int destY, int destWidth, int destHeight,
        SpecularResources<ContextType> readSource, SourceType srcTex, int srcX, int srcY, int srcWidth, int srcHeight,
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
     * @param destX The left edge of the rectangle to copy into within this blittable.
     * @param destY The bottom edge of the rectangle to copy into within this blittable.
     * @param destWidth The width of the rectangle to copy at the destination resolution.
     * @param destHeight The height of the rectangle to copy at the destination resolution.
     * @param readSource The blittable source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param srcWidth The width of the rectangle to copy at the source resolution.
     * @param srcHeight The height of the rectangle to copy at the source resolution.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     */
    default void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight,
        SpecularResources<ContextType> readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
    {
        // Blit each individual texture -- diffuse, normal, specular reflectivity, specular roughness, weight maps, weight mask
        blitCroppedAndScaledSingle(this.getDiffuseMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getDiffuseMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getNormalMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getNormalMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getConstantMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getConstantMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
//        blitCroppedAndScaledSingle(this.getQuadraticMap(), destX, destY, destWidth, destHeight,
//            readSource, readSource.getQuadraticMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getSpecularReflectivityMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getSpecularReflectivityMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getSpecularRoughnessMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getSpecularRoughnessMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getAlbedoMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getAlbedoMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        blitCroppedAndScaledSingle(this.getORMMap(), destX, destY, destWidth, destHeight,
            readSource, readSource.getORMMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);

        if (this.getOcclusionMap() != this.getORMMap() && readSource.getOcclusionMap() != this.getOcclusionMap())
        {
            blitCroppedAndScaledSingle(this.getOcclusionMap(), destX, destY, destWidth, destHeight,
                readSource, readSource.getOcclusionMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        }

        if (this.getBasisWeightResources() != null && readSource.getBasisWeightResources() != null)
        {
            blitCroppedAndScaledSingle(this.getBasisWeightResources().weightMaps, destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().weightMaps, srcX, srcY, srcWidth, srcHeight, linearFiltering);
            blitCroppedAndScaledSingle(this.getBasisWeightResources().weightMask, destX, destY, destWidth, destHeight,
                readSource, readSource.getBasisWeightResources().weightMask, srcX, srcY, srcWidth, srcHeight, linearFiltering);
        }
    }

    @Override
    default int getWidth()
    {
        return getAlbedoMap() == null ? 0 : getAlbedoMap().getWidth();
    }

    @Override
    default int getHeight()
    {
        return getAlbedoMap() == null ? 0 : getAlbedoMap().getHeight();
    }
}
