package tetzlaff.gl.interactive;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;

/**
 * An interface used with the InteractiveGraphics object to coordinate the initialization,
 * updating, drawing and deleting of an OpenGL-like renderable view.
 * 
 * @author Michael Tetzlaff
 * @see InteractiveGraphics
 */
public interface InteractiveRenderable<ContextType extends Context<ContextType>> extends AutoCloseable
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
     * The associated context will be made current first.
     * This method may also be called without subsequently calling draw() to allow its internal state to be updated only.
     */
    void update();

    /**
     * Interpret the internal state and draw this object.  Called every time the associated
     * InteractiveApplication object refreshes and immediately after update is called.
     * The associated context will be made current first.  Generally, the object should be
     * immutable (no internal state should change) while executing this method.
     */
    void draw(Framebuffer<ContextType> framebuffer);

    /**
     * Execute any cleanup and bring the internal state out of being prepared to draw. Update
     * and draw will not execute after this method without initialize first being called. Called
     * once by the associated InteractiveApplication created by InteractiveGraphics when the
     * application is terminating.  The associated context will be made current first.
     */
    @Override
    void close();
}
