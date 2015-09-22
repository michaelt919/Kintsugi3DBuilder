package tetzlaff.window.listeners;

import tetzlaff.window.Window;

/**
 * A listener for when a the size of the default framebuffer changes.
 * @author Michael Tetzlaff
 *
 */
public interface FramebufferSizeListener 
{
	/**
	 * Called when the size of the default framebuffer has changed.
	 * @param window The window whose framebuffer has changed.
	 * @param width The new width of the framebuffer, in true pixels.
	 * @param height The new height of the framebuffer, in true pixels.
	 */
	void framebufferResized(Window window, int width, int height);
}
