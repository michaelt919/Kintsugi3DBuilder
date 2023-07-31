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

package kintsugi3d.builder.fit;

import kintsugi3d.gl.core.Blittable;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;

public interface SpecularResources<ContextType extends Context<ContextType>> extends AutoCloseable, Blittable<SpecularResources<ContextType>>
{
    Texture2D<ContextType> getDiffuseMap();
    Texture2D<ContextType> getNormalMap();
    Texture2D<ContextType> getConstantMap();
    Texture2D<ContextType> getSpecularReflectivityMap();
    Texture2D<ContextType> getSpecularRoughnessMap();
    BasisResources<ContextType> getBasisResources();
    BasisWeightResources<ContextType> getBasisWeightResources();

    @Override
    void close(); // no exception

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
        this.getDiffuseMap().blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource.getDiffuseMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        this.getNormalMap().blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource.getNormalMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        this.getConstantMap().blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource.getConstantMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        this.getSpecularReflectivityMap().blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource.getSpecularReflectivityMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        this.getSpecularRoughnessMap().blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource.getSpecularRoughnessMap(), srcX, srcY, srcWidth, srcHeight, linearFiltering);
        this.getBasisWeightResources().weightMaps.blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource.getBasisWeightResources().weightMaps, srcX, srcY, srcWidth, srcHeight, linearFiltering);
        this.getBasisWeightResources().weightMask.blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource.getBasisWeightResources().weightMask, srcX, srcY, srcWidth, srcHeight, linearFiltering);
    }
}
