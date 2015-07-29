package tetzlaff.gl;

import java.io.File;
import java.io.IOException;

/**
 * An interface for a framebuffer.
 * This could be either the default on-screen framebuffer, or a framebuffer object (FBO).
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the framebuffer is associated with.
 */
public interface Framebuffer<ContextType extends Context<ContextType>> extends Contextual<ContextType>
{
	FramebufferSize getSize();

	int[] readColorBufferARGB(int attachmentIndex);
	int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height);

	float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height);
	float[] readFloatingPointColorBufferRGBA(int attachmentIndex);

	short[] readDepthBuffer(int x, int y, int width, int height);
	short[] readDepthBuffer();

	void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException;
	void saveColorBufferToFile(int attachmentIndex, int x, int y, int width, int height, String fileFormat, File file) throws IOException;

	void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a);
	void clearDepthBuffer(float depth);
	void clearDepthBuffer();
	void clearStencilBuffer(int stencilIndex);
}