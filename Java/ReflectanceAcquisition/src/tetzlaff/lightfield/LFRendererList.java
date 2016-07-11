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
package tetzlaff.lightfield;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.helpers.Trackball;

/**
 * An implementation for rendering a list of light fields that supports adding new light fields to the list as LFRenderer objects.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that will be used for rendering.
 */
public class LFRendererList<ContextType extends Context<ContextType>> extends LFDrawableListModel<ContextType>
{
	private static final long serialVersionUID = -8199166231586786343L;

	/**
	 * Creates a new light field renderer list.
	 * @param context The GL context in which to perform the rendering.
	 * @param trackball The trackball controlling the movement of the virtual camera.
	 */
	public LFRendererList(ContextType context, Trackball trackball) 
	{
		super(context, trackball);
	}
	
	@Override
	protected LFRenderer<ContextType> createFromVSETFile(File vsetFile, LFLoadOptions loadOptions) throws IOException
	{
		return new LFRenderer<ContextType>(context, this.getProgram(), vsetFile, loadOptions, trackball);
	}
	
	@Override
	protected LFRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, LFLoadOptions loadOptions) throws IOException
	{
		return new LFRenderer<ContextType>(context, this.getProgram(), xmlFile, meshFile, loadOptions, trackball);
	}
	
	@Override
	protected LFMorphRenderer<ContextType> createMorphFromLFMFile(File lfmFile, LFLoadOptions loadOptions) throws IOException
	{
		return new LFMorphRenderer<ContextType>(context, this.getProgram(), lfmFile, loadOptions, trackball);
	}
}
