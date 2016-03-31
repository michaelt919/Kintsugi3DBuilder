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
	/**
	 * Gets whether or not the framebuffer is framebuffer-complete.
	 * @return true if the framebuffer is complete, false if it is incomplete.
	 */
	boolean isComplete();

	/**
	 * Throws a runtime exception if the framebuffer is not framebuffer complete.
	 */
	void assertComplete();
	
	/**
	 * Gets the dimensions of the framebuffer (width and height)
	 * @return An object containing the framebuffer dimensions.
	 */
	FramebufferSize getSize();

	/**
	 * Reads the pixels currently in one of the framebuffer's attachments.
	 * The entire framebuffer will be read.
	 * @param attachmentIndex The index of the framebuffer attachment to be read.
	 * @return An array containing the pixels.
	 * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, read, green, and blue channels, respectively, from highest order byte to lowest.
	 */
	int[] readColorBufferARGB(int attachmentIndex);
	
	/**
	 * Reads the pixels currently in one of the framebuffer's attachments.
	 * Only a rectangular subset of the pixels will be read.
	 * @param attachmentIndex The index of the framebuffer attachment to be read.
	 * @param x The column at which to begin reading.
	 * @param y The row at which to begin reading.
	 * @param width The number of columns to read.
	 * @param height The number of rows to read.
	 * @return An array containing the pixels.
	 * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, read, green, and blue channels, respectively, from highest order byte to lowest.
	 */
	int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height);

	/**
	 * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
	 * Only a rectangular subset of the pixels will be read.
	 * @param attachmentIndex The index of the framebuffer attachment to be read.
	 * @param x The column at which to begin reading.
	 * @param y The row at which to begin reading.
	 * @param width The number of columns to read.
	 * @param height The number of rows to read.
	 * @return An array containing the pixels as floating point numbers.
	 */
	float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height);
	
	/**
	 * Reads the pixels currently in one of the framebuffer's attachments as floating point numbers.
	 * The entire framebuffer will be read.
	 * @param attachmentIndex The index of the framebuffer attachment to be read.
	 * @return An array containing the pixels as floating point numbers.
	 */
	float[] readFloatingPointColorBufferRGBA(int attachmentIndex);

	/**
	 * Reads the pixels currently in the framebuffer's depth attachment.
	 * Only a rectangular subset of the pixels will be read.
	 * @param x The column at which to begin reading.
	 * @param y The row at which to begin reading.
	 * @param width The number of columns to read.
	 * @param height The number of rows to read.
	 * @return An array containing the depth values.
	 */
	short[] readDepthBuffer(int x, int y, int width, int height);
	
	/**
	 * Reads the pixels currently in the framebuffer's depth attachment.
	 * The entire framebuffer will be read.
	 * @return An array containing the depth values.
	 */
	short[] readDepthBuffer();

	/**
	 * Saves the the pixels currently in one of the framebuffer's attachments to a file.
	 * The entire framebuffer will be read.
	 * @param attachmentIndex The index of the framebuffer attachment to be read.
	 * @param fileFormat The format of the file to be written.
	 * @param file The file to be written.
	 * @throws IOException Thrown if any file I/O problems occur when writing the file.
	 */
	void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException;
	
	/**
	 * Saves the the pixels currently in one of the framebuffer's attachments to a file.
	 * Only a rectangular subset of the pixels will be read.
	 * @param attachmentIndex The index of the framebuffer attachment to be read.
	 * @param x The column at which to begin reading.
	 * @param y The row at which to begin reading.
	 * @param width The number of columns to read.
	 * @param height The number of rows to read.
	 * @param fileFormat The format of the file to be written.
	 * @param file The file to be written.
	 * @throws IOException Thrown if any file I/O problems occur when writing the file.
	 */
	void saveColorBufferToFile(int attachmentIndex, int x, int y, int width, int height, String fileFormat, File file) throws IOException;
	
	/**
	 * Clears one of the framebuffer's color attachments.
	 * @param attachmentIndex The index of the framebuffer attachment to be cleared.
	 * @param r The red component of the clear color.
	 * @param g The green component of the clear color.
	 * @param b The blue component of the clear color.
	 * @param a The alpha component of the clear color.
	 */
	void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a);

	/**
	 * Clears one of the framebuffer's color attachments.
	 * @param attachmentIndex The index of the framebuffer attachment to be cleared.
	 * @param r The red component of the clear color.
	 * @param g The green component of the clear color.
	 * @param b The blue component of the clear color.
	 * @param a The alpha component of the clear color.
	 */
	void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a);
	
	/**
	 * Clears the framebuffer's depth attachment.
	 * @param depth The depth at which to clear.
	 */
	void clearDepthBuffer(float depth);
	
	/**
	 * Clears the framebuffer's depth attachment to the far plane distance.
	 */
	void clearDepthBuffer();
	
	/**
	 * Clears the framebuffer's stencil buffer.
	 * @param stencilIndex The stencil index with which to clear.
	 */
	void clearStencilBuffer(int stencilIndex);
}