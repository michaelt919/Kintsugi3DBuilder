package tetzlaff.gl.opengl;

import tetzlaff.gl.FramebufferAttachment;

interface OpenGLFramebufferAttachment extends FramebufferAttachment<OpenGLContext>
{
    void attachToDrawFramebuffer(int attachment, int level);
    void attachToReadFramebuffer(int attachment, int level);
}
