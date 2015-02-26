package tetzlaff.gl;

public interface Context 
{
	boolean isDestroyed();
	
	void makeContextCurrent();

	void flush();
	void swapBuffers();
	
	void destroy();

	FramebufferSize getFramebufferSize();
	
	void enableDepthTest();
	void disableDepthTest();
	
	void enableMultisampling();
	void disableMultisampling();
	
	void enableBackFaceCulling();
	void disableBackFaceCulling();

	void setAlphaBlendingFunction(AlphaBlendingFunction func);
	void disableAlphaBlending();
}
