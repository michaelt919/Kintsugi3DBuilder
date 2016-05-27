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
 * Thrown when an attempt is made to use a shader program that has not been linked (or failed to link).
 * @author Michael Tetzlaff
 *
 */
public class UnlinkedProgramException extends IllegalStateException 
{
	private static final long serialVersionUID = 7045695222299534322L;

	public UnlinkedProgramException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UnlinkedProgramException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public UnlinkedProgramException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public UnlinkedProgramException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
