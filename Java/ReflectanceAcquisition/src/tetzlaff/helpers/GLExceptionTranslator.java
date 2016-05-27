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
package tetzlaff.helpers;

import java.io.FileNotFoundException;

import tetzlaff.gl.exceptions.GLException;

/**
 * An exception translator designed for graphics applications.
 * Errors are classified as either graphics/GL, resource (File I/O), or general/application errors.
 * @author Michael Tetzlaff
 *
 */
public class GLExceptionTranslator implements ExceptionTranslator 
{
	@Override
	public ErrorMessage translate(Exception e) 
	{
		if(e instanceof GLException || (e.getCause() != null && e.getCause() instanceof GLException))
		{
			return new ErrorMessage("GL Rendering Error", "An error occured with the rendering system. " +
					"Your GPU and/or video memory may be insufficient for rendering this model.\n\n[" +
					e.getMessage() + "]");
		}
		else if(e instanceof FileNotFoundException || (e.getCause() != null && e.getCause() instanceof FileNotFoundException))
		{
			return new ErrorMessage("Resource Error", "An error occured while loading resources. " +
					"Check that all necessary files exist and that the proper paths were supplied.\n\n[" +
					e.getMessage() + "]");
		}
		else
		{
			return new ErrorMessage("Application Error", "An error occured that prevents this model from being rendered." +
					"\n\n[" + e.getMessage() + "]");
		}
	}
	
}
