package tetzlaff.gl.helpers;

/**
 * An interfaced used with the InteractiveGraphics object to coordinate the initialization,
 * updating, drawing and deleting of an OpenGL like renderable view.
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
	 * associated InteractiveApplication object refreshes and immediately before draw is called.
	 * The associated context will be made current first.
	 */
	void update();
	
	/**
	 * Interpret the internal state and draw this object.  Called every time the associated
	 * InteractiveApplication object refreshes and immediately after update is called.
	 * The associated context will be made current first.  Generally, the object should be
	 * immutable (no internal state should change) while executing this method.
	 */
	void draw();
	
	/**
	 * Execute any cleanup and bring the internal state out of being prepared to draw. Update
	 * and draw will not execute after this method without initialize first being called. Called
	 * once by the associated InteractiveAppliction created by InteractiveGraphics when the
	 * application is terminating.  The associated context will be made current first.
	 */
	void cleanup();
}
