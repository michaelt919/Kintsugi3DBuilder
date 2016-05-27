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
 * Thrown when a shader fails to compile.
 * @author Michael Tetzlaff
 *
 */
public class ShaderCompileFailureException extends RuntimeException 
{
	private static final long serialVersionUID = 7556469381337373536L;

	public ShaderCompileFailureException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
