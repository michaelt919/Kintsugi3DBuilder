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

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.DepthTextureBuilder;

public abstract class DepthTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
	extends TextureBuilderBase<ContextType, TextureType> implements DepthTextureBuilder<ContextType, TextureType>
{
	private int precision = 8;
	private boolean floatingPoint;
	
	protected int getInternalPrecision()
	{
		return this.precision;
	}
	
	protected boolean isFloatingPointEnabled()
	{
		return this.floatingPoint;
	}
	
	protected DepthTextureBuilderBase(ContextType context)
	{
		super(context);
	}
	
	@Override
	public DepthTextureBuilderBase<ContextType, TextureType> setInternalPrecision(int precision)
	{
		this.precision = precision;;
		return this;
	}
	
	@Override
	public DepthTextureBuilderBase<ContextType, TextureType> setFloatingPointEnabled(boolean enabled)
	{
		this.floatingPoint = enabled;
		return this;
	}
}
