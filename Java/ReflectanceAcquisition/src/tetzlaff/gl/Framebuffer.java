package tetzlaff.gl;

import java.io.IOException;

public interface Framebuffer
{
	int getWidth();
	int getHeight();

	int[] readColorBufferARGB(int attachmentIndex);
	int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height);

	void saveColorBufferToFile(int attachmentIndex, String fileFormat, String filename) throws IOException;

	void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a);
	void clearDepthBuffer(int attachmentIndex, float depth);
	void clearStencilBuffer(int attachmentIndex, int stencilIndex);
}