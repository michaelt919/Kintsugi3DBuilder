package tetzlaff.gl.helpers;

import java.io.File;

/**
 * An interface used with the InteractiveGraphics object to coordinate the initialization,
 * updating, drawing and deleting of an OpenGL-like renderable view.
 * 
 * @author Michael Tetzlaff
 * @see InteractiveGraphics
 */
public interface Drawable 
{
	/**
	 * Execute any initialization needed prior to updating and drawing this object.  You
	 * should bring the internal state into being prepared to call update and draw.  Called
	 * once by the associated InteractiveAppliction created by InteractiveGraphics.  The
	 * associated context will be made current first.
	 */
	void initialize();
	
	/**
	 * Adjust internal state that needs to change prior to drawing.  Called every time the
	 * associated InteractiveApplication object refreshes and before draw() is called.
	 * The associated context will be made current first.  This method may also be called
	 * without subsequently calling draw() to allow its internal state to be updated only.
	 */
	void update();
	
	/**
	 * Interpret the internal state and draw this object.  Called every time the associated
	 * InteractiveApplication object refreshes and immediately after update is called.
	 * The associated context will be made current first.  Generally, the object should be
	 * immutable (no internal state should change) while executing this method.
	 */
	boolean draw();

	/**
	 * Save the contents of this Drawable to a file.  This should be called after draw() and
	 * after the context has been flushed if you want to save the drawn results to a file.
	 */
	void saveToFile(String fileFormat, File file);
	
	/**
	 * Execute any cleanup and bring the internal state out of being prepared to draw. Update
	 * and draw will not execute after this method without initialize first being called. Called
	 * once by the associated InteractiveApplication created by InteractiveGraphics when the
	 * application is terminating.  The associated context will be made current first.
	 */
	void cleanup();
	
	/**
	 * Retrieve information about frames per second for this drawable
	 */
	default int getCurFPS() { return -1; }
	default int getMinFPS() { return -1; }
	default int getMaxFPS() { return -1; }
	
	default void resetFPSRange() {}
	
	/**
	 * During initialization if any errors occur, rather than having those be thrown from the
	 * initialize function they can be querying for later. Initialize can store any generated
	 * exception object and this method should return that object.  If no errors/exceptions
	 * occurred it should return null.  Note: the drawable that overrides this method might
	 * also automatically clear the error after it is retrieved.
	 * @return The exception object (if an exception occurred during initialize()) or null
	 */
	default Exception getInitializeError() { return null; }	

	/**
	 * Similar to getInitializeError() but only checks if one occurs.  This will not retrieve
	 * or reset the error.
	 * @return True if an error occurred during initialization, false otherwise
	 */
	default boolean hasInitializeError() { return true; }	
}
