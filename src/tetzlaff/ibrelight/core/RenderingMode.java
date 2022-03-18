package tetzlaff.ibrelight.core;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.vecmath.Matrix4;

public interface RenderingMode<ContextType extends Context<ContextType>>
{
    /**
     * Draw the object using the current settings and selections in IBRelight,
     * potentially in subdivisions to avoid graphics card timeouts.
     * @param framebuffer The framebuffer into which to draw the object.
     * @param view The view matrix.
     * @param projection The projection matrix.
     * @param subdivWidth The width of the rectangle of pixels to draw at once.  This can be set to a fraction of the
     *                    framebuffer width to reduce the likelihood of graphics card timeouts that would crash IBRelight.
     * @param subdivHeight The height of the rectangle of pixels to draw at once.  This can be set to a fraction of the
     *                     framebuffer height to reduce the likelihood of graphics card timeouts that would crash IBRelight.
     */
//    void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 projection, int subdivWidth, int subdivHeight);
}
