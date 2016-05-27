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
package tetzlaff.gl;

/**
 * Enumerates the possible ways in which a buffer could be read or written to with respect to the application or the shaders.
 * @author Michael Tetzlaff
 *
 */
public enum BufferAccessType 
{
	/**
	 * The buffer will be primarily written to by the application and read by shaders.
	 */
	DRAW,
	
	/**
	 * The buffer will be primarily written to by shaders and read by the application.
	 */
	READ,
	
	/**
	 * The buffer will be primarily both written to and read from shaders.
	 */
	COPY
}
