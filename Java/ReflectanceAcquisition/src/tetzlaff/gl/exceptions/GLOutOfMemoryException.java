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
package tetzlaff.gl.exceptions;

/**
 * Thrown when the GL implementation runs out of memory.
 * The state of the GL will be undefined if this is thrown 
 * (meaning that the context has no further use and should be be destroyed).
 * @author Michael Tetzlaff
 *
 */
public class GLOutOfMemoryException extends GLException 
{
	private static final long serialVersionUID = -1977989622291033976L;

	public GLOutOfMemoryException() 
	{
		super("There is not enough memory left to execute the command.  The state of the GL is undefined.");
	}
}
