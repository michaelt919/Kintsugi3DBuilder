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

package kintsugi3d.gl.core;

public interface Blittable<SourceType extends Blittable<?>>
{
    /**
     * Gets the width of the blittable.
     * @return The width of the blittable.
     */
    int getWidth();

    /**
     * Gets the height of the blittable.
     * @return The height of the blittable.
     */
    int getHeight();

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
     *                        This may be ignored if linear filtering is not supported (i.e. for depth / stencil textures).
     */
    void blitCroppedAndScaled(int destX, int destY, int destWidth, int destHeight,
        SourceType readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering);

    /**
     * Copies pixels from one blittable to another.  The copying operation will be start at (x, y) within
     * this blittable, and resize if the requested destination rectangle is not the same size as the source blittable.
     * @param destX The left edge of the rectangle to copy into within this blittable.
     * @param destY The bottom edge of the rectangle to copy into within this blittable.
     * @param destWidth The width of the rectangle to copy at the destination resolution.
     * @param destHeight The height of the rectangle to copy at the destination resolution.
     * @param readSource The blittable source to copy from.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     *                        This may be ignored if linear filtering is not supported (i.e. for depth / stencil textures).
     */
    default void blitScaled(int destX, int destY, int destWidth, int destHeight, SourceType readSource, boolean linearFiltering)
    {
        blitCroppedAndScaled(destX, destY, destWidth, destHeight,
            readSource, 0, 0, readSource.getWidth(), readSource.getHeight(), linearFiltering);
    }

    /**
     * Copies pixels from one blittable to another.
     * The copying operation will span the entirety of both blittables, resizing it their resolutions are not the same.
     * @param readSource The blittable source to copy from.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     *                        This may be ignored if linear filtering is not supported (i.e. for depth / stencil textures).
     */
    default void blitScaled(SourceType readSource, boolean linearFiltering)
    {
        blitScaled(0, 0, this.getWidth(), this.getHeight(), readSource, linearFiltering);
    }

    /**
     * Copies pixels from one blittable to another.  The copying operation will be start at (x, y) within
     * this blittable, and resize if the requested destination rectangle is not the same size as the source blittable.
     * @param readSource The blittable source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param srcWidth The width of the rectangle to copy at the source resolution.
     * @param srcHeight The height of the rectangle to copy at the source resolution.
     * @param linearFiltering Whether or not to use linear filtering if the dimensions of the source and destination are not the same.
     *                        This may be ignored if linear filtering is not supported (i.e. for depth / stencil textures).
     */
    default void blitCroppedAndScaled(SourceType readSource, int srcX, int srcY, int srcWidth, int srcHeight, boolean linearFiltering)
    {
        blitCroppedAndScaled(0, 0, this.getWidth(), this.getHeight(),
            readSource, srcX, srcY, srcWidth, srcHeight, linearFiltering);
    }

    /**
     * Copies pixels from part of a blittable to another.
     * The copying operation will be start at (x, y) within this blittable, and will preserve the resolution of the read source.
     * @param destX The left edge of the rectangle to copy into within this blittable.
     * @param destY The bottom edge of the rectangle to copy into within this blittable.
     * @param readSource The blittable source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param width The width of the rectangle to copy.
     * @param height The height of the rectangle to copy.
     */
    default void blitCropped(int destX, int destY, SourceType readSource, int srcX, int srcY, int width, int height)
    {
        blitCroppedAndScaled(destX, destY, width, height, readSource, srcX, srcY, width, height, false);
    }

    /**
     * Copies pixels from part of a blittable to another.
     * The copying operation will be start at (0, 0) within this blittable, and will preserve the resolution of the read source.
     * @param readSource The blittable source to copy from.
     * @param srcX The left edge of the rectangle to copy from within the source.
     * @param srcY The bottom edge of the rectangle to copy from within the source.
     * @param width The width of the rectangle to copy.
     * @param height The height of the rectangle to copy.
     */
    default void blitCropped(SourceType readSource, int srcX, int srcY, int width, int height)
    {
        blitCroppedAndScaled(0, 0, width, height, readSource, srcX, srcY, width, height, false);
    }

    /**
     * Copies pixels from one blittable to another.
     * The copying operation will be start at (x, y) within this blittable, and will preserve the resolution of the read source
     * @param x The left edge of the rectangle to copy into within this blittable.
     * @param y The bottom edge of the rectangle to copy into within this blittable.
     * @param readSource The blittable source to copy from.
     */
    default void blit(int x, int y, SourceType readSource)
    {
        blitCropped(x, y, readSource, 0, 0, readSource.getWidth(), readSource.getHeight());
    }

    /**
     * Copies pixels from one blittable to another.
     * The copying operation will be start at the lower left corner of this blittable, and will preserve the resolution of the read source
     * @param readSource The blittable source to copy from.
     */
    default void blit(SourceType readSource)
    {
        blit(0, 0, readSource);
    }
}
