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
import tetzlaff.gl.builders.TextureBuilder;

public abstract class TextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> implements TextureBuilder<ContextType, TextureType>
{
	protected final ContextType context;
	private int multisamples = 1;
	private boolean fixedMultisampleLocations = true;
	private boolean mipmapsEnabled = false;
	private boolean linearFilteringEnabled = false;

	
	protected int getMultisamples()
	{
		return multisamples;
	}
	
	protected boolean areMultisampleLocationsFixed()
	{
		return fixedMultisampleLocations;
	}
	
	protected boolean areMipmapsEnabled()
	{
		return mipmapsEnabled;
	}
	
	protected boolean isLinearFilteringEnabled()
	{
		return linearFilteringEnabled;
	}
	
	protected TextureBuilderBase(ContextType context)
	{
		this.context = context;
	}
	
	@Override
	public TextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
	{
		multisamples = samples;
		fixedMultisampleLocations = fixedSampleLocations;
		return this;
	}
	
	@Override
	public TextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
	{
		mipmapsEnabled = enabled;
		return this;
	}
	
	@Override
	public TextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
	{
		linearFilteringEnabled = enabled;
		return this;
	}
}
