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
 * An interface intended to be used to abstract away the details of the Message
 * box system of a standard GUI API of your choice.  The four methods provided
 * can be used to communicate with the user.
 *  
 * @author berriers
 */
public interface MessageBox
{
	/**
	 * Some simple values to encapsulate responses from the user for a Question
	 * dialog.
	 * 
	 * @author berriers
	 */
	public enum Response
	{
		YES,
		NO,
		CANCEL
	}	
	
	/**
	 * Show a dialog box that is application modal with a title in the window bar,
	 * a message, and an OK button.  This function will block until the user dismisses
	 * the dialog.
	 * @param title Text to show in the window title bar.
	 * @param message The main message in the dialog window. 
	 */
	void info(String title, String message);
	
	/**
	 * Show a dialog box that asks the user a Yes/No question with text in the
	 * window title bar, a message, and three buttons (Yes, No, and Cancel).  This
	 * Function will block execution until the user dismisses the dialog.
	 * @param title Text to show in the window title bar.
	 * @param message The main message in the dialog window. 
	 * @return The identity of the button clicked as a Response enum.
	 */
	Response question(String title, String message);

	/**
	 * Show a dialog box that is application modal intended to warn the user about a
	 * recoverable error.  It will have an icon to match the nature of the warning as
	 * well as text in the window title bar, a message, and an OK button.  This
	 * function will block until the user dismisses the dialog.
	 * @param title Text to show in the window title bar.
	 * @param message The main message in the dialog window.
	 */	
	void warning(String title, String message);

	/**
	 * Show a dialog box that is application modal intended to warn the user about a
	 * critical error.  It will have an icon to match the nature of the warning as
	 * well as text in the window title bar, a message, and an OK button.  This
	 * function will block until the user dismisses the dialog.
	 * @param title Text to show in the window title bar.
	 * @param message The main message in the dialog window. 
	 */	
	void error(String title, String message);
}
