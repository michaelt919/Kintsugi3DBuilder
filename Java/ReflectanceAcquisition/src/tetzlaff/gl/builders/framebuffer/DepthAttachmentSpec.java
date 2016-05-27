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
package tetzlaff.gl.builders.framebuffer;

/**
 * A builder-like object for specifying a framebuffer depth attachment.
 * @author Michael Tetzlaff
 *
 */
public class DepthAttachmentSpec extends AttachmentSpec
{
	/**
	 * The number of bits to use to represent each depth value.
	 */
	public final int precision;
	
	/**
	 * Whether or not floating-point storage for this depth attachment.
	 */
	public final boolean floatingPoint;
	
	/**
	 * Creates a new depth attachment specification.
	 * @param precision The number of bits to use to represent each depth value.
	 * @param floatingPoint Whether or not floating-point storage for this depth attachment.
	 */
	public DepthAttachmentSpec(int precision, boolean floatingPoint)
	{
		this.precision = precision;
		this.floatingPoint = floatingPoint;
	}
	
	@Override
	public DepthAttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
	{
		super.setMultisamples(samples, fixedSampleLocations);
		return this;
	}

	@Override
	public DepthAttachmentSpec setMipmapsEnabled(boolean enabled)
	{
		super.setMipmapsEnabled(enabled);
		return this;
	}
	
	@Override
	public DepthAttachmentSpec setLinearFilteringEnabled(boolean enabled)
	{
		super.setLinearFilteringEnabled(enabled);
		return this;
	}
}
