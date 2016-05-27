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
package tetzlaff.gl.builders.base;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.ColorTextureBuilder;

public abstract class ColorTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
	extends TextureBuilderBase<ContextType, TextureType> implements ColorTextureBuilder<ContextType, TextureType>
{
	private ColorFormat internalColorFormat = ColorFormat.RGBA8;
	private CompressionFormat internalCompressionFormat = null;
	
	protected ColorFormat getInternalColorFormat()
	{
		return internalColorFormat;
	}
	
	protected CompressionFormat getInternalCompressionFormat()
	{
		return internalCompressionFormat;
	}
	
	protected boolean isInternalFormatCompressed()
	{
		return internalCompressionFormat != null;
	}
	
	protected ColorTextureBuilderBase(ContextType context)
	{
		super(context);
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setInternalFormat(ColorFormat format)
	{
		internalColorFormat = format;
		internalCompressionFormat = null;
		return this;
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setInternalFormat(CompressionFormat format)
	{
		internalColorFormat = null;
		internalCompressionFormat = format;
		return this;
	}
}
