package openGL.wrappers.interfaces;

public interface FramebufferAttachment 
{
	void bind();
	void attachToDrawFramebuffer(int attachment, int level);
	void attachToReadFramebuffer(int attachment, int level);
	void delete();
}
