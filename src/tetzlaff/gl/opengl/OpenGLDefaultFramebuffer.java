package tetzlaff.gl.opengl;

import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.core.FramebufferSize;

import static org.lwjgl.opengl.GL11.*;

class OpenGLDefaultFramebuffer extends OpenGLFramebuffer implements DoubleFramebuffer<OpenGLContext>
{
    private final ContentsImpl contentsImpl = new ContentsImpl();

    OpenGLDefaultFramebuffer(OpenGLContext context)
    {
        super(context);
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
    public ContentsImpl getContentsForRead()
    {
        return contentsImpl;
    }

    @Override
    public ContentsImpl getContentsForWrite()
    {
        return contentsImpl;
    }

    private class ContentsImpl extends ContentsBase
    {
        @Override
        int getId()
        {
            return 0;
        }

        @Override
        void selectColorSourceForRead(int index)
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
}
