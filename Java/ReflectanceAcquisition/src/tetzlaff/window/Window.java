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
package tetzlaff.window;


/**
 * An interface for any window created for graphics use.
 * @author Michael Tetzlaff
 *
 */
public interface Window extends WindowListenerManager
{
	/**
	 * Makes the window visible.
	 */
	void show();

	/**
	 * Makes the window invisible.
	 */
	void hide();
	
	/**
	 * Gets whether or not the window is "high DPI" (the actual size of the framebuffer is larger than the logical size of the window).
	 * @return true if the window is high-DPI, false otherwise.
	 */
	boolean isHighDPI();
	
	/**
	 * Gets whether or not the window is about to be closed.
	 * @return true if the window is about to close, false otherwise.
	 */
	boolean isClosing();
	
	/**
	 * Requests that the window be closed.
	 * After this method has been called, all other methods will have undefined results.
	 */
	void requestClose();
	
	/**
	 * Cancels a window close operation.
	 * If this is called before the window is actually closed, the window will not be closed.
	 */
	void cancelClose();
	
	/**
	 * Gets the logical size of the window.
	 * @return The logical size of the window.
	 */
	WindowSize getWindowSize();

	/**
	 * Gets the position of the window on the screen.
	 * @return The position of the window on the screen.
	 */
	WindowPosition getWindowPosition();

	/**
	 * Sets the title of the window.
	 * @param title The title of the window.
	 */
	void setWindowTitle(String title);

	/**
	 * Sets the logical size of the window.
	 * @param width The desired width of the window.
	 * @param height The desired height of the window.
	 */
	void setWindowSize(int width, int height);

	/**
	 * Sets the position of the window on the screen.
	 * @param x The x-coordinate of the window position.
	 * @param y The y-coordinate of the window position.
	 */
	void setWindowPosition(int x, int y);
	
	/**
	 * Gets the state of a mouse button (pressed or released).
	 * @param buttonIndex The index of the mouse button to query.
	 * 0 is generally used for the left mouse button, 1 for the right, and 2 for the middle.
	 * @return The state of the mouse button.
	 */
	MouseButtonState getMouseButtonState(int buttonIndex);
	
	/**
	 * Gets the state of a key (pressed or released).
	 * @param keycode The keycode of the key to query (see tetzlaff.window.KeyCodes)
	 * @return The state of the key.
	 */
	KeyState getKeyState(int keycode);
	
	/**
	 * Gets the position of the cursor on the screen.
	 * @return The position of the cursor on the screen.
	 */
	CursorPosition getCursorPosition();
	
	/**
	 * Gets the state of the modifier keys.
	 * @return The state of the modifier keys.
	 */
	ModifierKeys getModifierKeys();
}
