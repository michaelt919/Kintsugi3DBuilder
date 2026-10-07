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

package kintsugi3d.gl.core;

public interface ReadonlyTexture3D<ContextType extends Context<ContextType>>
    extends Texture<ContextType>, Croppable<Texture3D<ContextType>>, ThreeDimensional
{
    /**
     * Gets the width of the texture.
     * @return The width of the texture.
     */
    @Override
    int getWidth();

    /**
     * Gets the height of the texture.
     * @return The height of the texture.
     */
    @Override
    int getHeight();

    /**
     * Gets the depth of the texture (the number of layers if used as a texture array).
     * @return The depth of the texture.
     */
    @Override
    int getDepth();

    /**
     * Creates a new, empty texture with different dimensions but the same internal format and settings as this texture.
     * Especially intended to be used with framebuffer blitting to ensure compatibility.
     *
     * @param newWidth  The width of the new texture.
     * @param newHeight The height of the new texture.
     * @param newDepth  The depth of the new texture.
     * @return The newly created texture.
     */
    Texture3D<ContextType> createTextureWithMatchingFormat(int newWidth, int newHeight, int newDepth);

    /**
     * Creates a new texture that contains a cropped 3D region of this texture.
     * The texture this method is called on will remain unchanged.
     * @param x The left boundary of the cropped region
     * @param y The bottom boundary of the cropped region
     * @param z The lower depth boundary of the cropped region
     * @param cropWidth The width of the cropped region
     * @param cropHeight The height of the cropped region
     * @param cropDepth The depth of the cropped region
     * @return The new cropped texture.
     */
    default Texture3D<ContextType> crop(int x, int y, int z, int cropWidth, int cropHeight, int cropDepth)
    {
        Texture3D<ContextType> cropTexture = createTextureWithMatchingFormat(cropWidth, cropHeight, cropDepth);
        cropTexture.blitCropped(this, x, y, z, cropWidth, cropHeight, cropDepth);
        return cropTexture;
    }

    /**
     * Creates a new texture that contains a cropped 2D region of this texture.  All z-layers will be copied.
     * The texture this method is called on will remain unchanged.
     * @param x The left boundary of the cropped region
     * @param y The bottom boundary of the cropped region
     * @param cropWidth The width of the cropped region
     * @param cropHeight The height of the cropped region
     * @return The new cropped texture.
     */
    @Override
    default Texture3D<ContextType> crop(int x, int y, int cropWidth, int cropHeight)
    {
        return this.crop(x, y, 0, cropWidth, cropHeight, this.getDepth());
    }

    /**
     * Copies this texture, creating a new resource with identical contents.
     * @return The new resource containing a copy of the texture
     */
    default Texture3D<ContextType> copy()
    {
        return this.crop(0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * Gets an object that encapsulates read capabilities for this texture as a color texture.
     * @param layerIndex The index of the layer within the 3D texture to be read.
     * @return the texture reader
     */
    ColorTextureReader getColorTextureReader(int layerIndex);

    /**
     * Gets an object that encapsulates read capabilities for this texture as a depth texture.
     * @param layerIndex The index of the layer within the 3D texture to be read.
     * @return the texture reader
     */
    DepthTextureReader getDepthTextureReader(int layerIndex);
}
