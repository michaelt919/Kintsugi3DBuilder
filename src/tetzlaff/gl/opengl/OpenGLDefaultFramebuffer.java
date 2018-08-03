package tetzlaff.gl.opengl;

import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.core.FramebufferSize;

import static org.lwjgl.opengl.GL11.*;

class OpenGLDefaultFramebuffer extends OpenGLFramebuffer implements DoubleFramebuffer<OpenGLContext>
{
    OpenGLDefaultFramebuffer(OpenGLContext context)
    {
        super(context);
    }

    @Override
    protected int getId()
    {
        return 0;
    }

    @Override
    public FramebufferSize getSize()
    {
        return this.context.getDefaultFramebufferSize();
    }

    @Override
    public void swapBuffers()
    {
        this.context.swapDefaultFramebuffer();
    }

    @Override
    protected void selectColorSourceForRead(int index)
    {
        if (index == 0)
        {
            glReadBuffer(GL_BACK);
            OpenGLContext.errorCheck();
        }
        else
        {
            throw new IllegalArgumentException("The default framebuffer does not have multiple color attachments.");
        }
    }
}
