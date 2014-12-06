package tetzlaff.gl.opengl;


interface OpenGLFramebufferAttachment extends OpenGLResource
{
	void attachToDrawFramebuffer(int attachment, int level);
	void attachToReadFramebuffer(int attachment, int level);
}
