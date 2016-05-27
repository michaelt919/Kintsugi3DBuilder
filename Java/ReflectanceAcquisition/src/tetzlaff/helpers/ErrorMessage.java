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

/**
 * Represents a "user-friendly" error message for when something goes wrong.
 * @author Michael Tetzlaff
 *
 */
public class ErrorMessage 
{
	/**
	 * The title of the error.
	 */
	public final String title;
	
	/**
	 * The message containing a detailed description of the error.
	 */
	public final String message;
	
	/**
	 * Creates a new error message.
	 * @param title The title of the error.
	 * @param message The message containing a detailed description of the error.
	 */
	public ErrorMessage(String title, String message)
	{
		this.title = title;
		this.message = message;
	}
}
