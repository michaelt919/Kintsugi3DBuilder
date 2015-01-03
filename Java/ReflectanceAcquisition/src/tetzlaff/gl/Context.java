package tetzlaff.gl;

public interface Context 
{
	boolean isDestroyed();
	
	void makeContextCurrent();

	void swapBuffers();
	
	void destroy();

	FramebufferSize getFramebufferSize();
	
	void enableDepthTest();
	void disableDepthTest();
	
	void enableMultisampling();
	void disableMultisampling();
}
