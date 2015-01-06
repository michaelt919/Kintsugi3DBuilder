package tetzlaff.gl.opengl;


public interface OpenGLFramebufferAttachment
{
	void attachToDrawFramebuffer(int attachment, int level);
	void attachToReadFramebuffer(int attachment, int level);
}
