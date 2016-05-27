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
package tetzlaff.interactive;

import java.io.File;

/**
 * An interface for an object that needs to be regularly refreshed.
 * @author Michael Tetzlaff
 *
 */
public interface Refreshable 
{
	/**
	 * Initializes the object.  This method should only ever be called once in an object's lifetime.
	 */
	void initialize();
	
	/**
	 * Refreshes the object.  This method should be called as often as possible until the object is destroyed.
	 */
	void refresh();
	
	/**
	 * Terminates the object.  Attempting to use an object after terminating it will have undefined results.
	 */
	void terminate();
	
	/**
	 * Requests that the object save a description of its current state to a particular file for debugging purposes.
	 * This request may be either handled by writing to the specified file in the requested format, or may be ignored.
	 * It is recommended that interactive graphics applications implement this method by saving a screenshot of the current framebuffer.
	 * @param fileFormat The desired format of the debug file to be written.
	 * @param file The debug file to write to.
	 */
	void requestDebugDump(String fileFormat, File file);
	
	/**
	 * Determines whether or not an error has occurred.
	 * @return true if an error has occurred, false otherwise.
	 */
	boolean hasError();
	
	/**
	 * Gets an exception representing the most recent error that has occurred, if any.
	 * Calling this method will attempt to reset the error state of the object.
	 * @return An exception representing the most error that has occurred, or null if no error has occurred.
	 */
	Exception getError();
}
