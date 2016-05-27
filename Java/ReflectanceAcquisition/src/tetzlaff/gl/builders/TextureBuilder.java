/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

/**
 * Implements the builder design pattern for creating textures.
 * Implementations should use implement one of the sub-interfaces ColorTextureBuilder, DepthTextureBuilder, StencilTextureBuilder, or DepthStencilTextureBuilder, as applicable.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture will be associated with.
 * @param <TextureType> The type of texture to be created.
 */
public interface TextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>>
{
	/**
	 * Sets the number of samples to use for multisampling.
	 * If the number of samples is 1, multisampling will be disabled for this texture.
	 * @param samples The number of samples to use.
	 * @param fixedSampleLocations Whether or not the sample locations are required to be fixed across all texels.
	 * @return The calling builder object.
	 */
	TextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
	
	/**
	 * Sets whether or not mipmaps should be enabled.
	 * If mipmaps are enabled, the texture will come with them pre-generated.
	 * If the texture is modified as the result of being used as a framebuffer attachment, the mipmaps will become stale and will need to be re-generated.
	 * @param enabled Whether or not mipmaps should be enabled.
	 * @return The calling builder object.
	 */
	TextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
	
	/**
	 * Sets whether linear filtering should be enabled.
	 * @param enabled true to enable linear filtering, false otherwise.
	 * @return The calling builder object.
	 */
	TextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
	
	/**
	 * Creates the texture.
	 * @return The newly created texture.
	 */
	TextureType createTexture();
}
