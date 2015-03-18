package tetzlaff.gl;

import java.io.File;
import java.io.IOException;

public interface Framebuffer
{
	FramebufferSize getSize();

	int[] readColorBufferARGB(int attachmentIndex);
	int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height);

	float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height);
	float[] readFloatingPointColorBufferRGBA(int attachmentIndex);

	short[] readDepthBuffer(int x, int y, int width, int height);
	short[] readDepthBuffer();

	void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException;

	void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a);
	void clearDepthBuffer(float depth);
	void clearDepthBuffer();
	void clearStencilBuffer(int stencilIndex);
}