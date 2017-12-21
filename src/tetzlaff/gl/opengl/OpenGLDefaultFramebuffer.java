package tetzlaff.gl.opengl;

import tetzlaff.gl.core.FramebufferSize;

import static org.lwjgl.opengl.GL11.*;

class OpenGLDefaultFramebuffer extends OpenGLFramebuffer
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
        return this.context.getFramebufferSize();
    }

    @Override
    protected void selectColorSourceForRead(int index)
    {
        if (index != 0)
        {
            throw new IllegalArgumentException("The default framebuffer does not have multiple color attachments.");
        }
        else
        {
            glReadBuffer(GL_BACK);
            this.context.openGLErrorCheck();
        }
    }
}
