/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import tetzlaff.gl.types.AbstractDataType;

/**
 * An interface for a three-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture3D<ContextType extends Context<ContextType>> extends Texture<ContextType>
{
    int getWidth();
    int getHeight();
    int getDepth();

    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT, TextureWrapMode wrapR);

    void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical) throws IOException;
    void loadLayer(int layerIndex, File file, boolean flipVertical) throws IOException;
    void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;
    void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical) throws IOException;
    <MappedType> void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;
    <MappedType> void loadLayer(int layerIndex, File file, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;
    <MappedType> void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;
    <MappedType> void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;
    void generateMipmaps();

    FramebufferAttachment<ContextType> getLayerAsFramebufferAttachment(int layerIndex);
}
