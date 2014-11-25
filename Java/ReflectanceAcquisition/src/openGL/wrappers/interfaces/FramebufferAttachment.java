package openGL.wrappers.interfaces;

public interface FramebufferAttachment extends GLResource
{
	void attachToDrawFramebuffer(int attachment, int level);
	void attachToReadFramebuffer(int attachment, int level);
}
