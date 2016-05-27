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
 * Thrown when an attempt is made to use more texture units than are available in the GL implementation.
 * @author Michael Tetzlaff
 *
 */
public class NoAvailableTextureUnitsException extends RuntimeException {

	private static final long serialVersionUID = -3881373761882575026L;

	public NoAvailableTextureUnitsException() {
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
