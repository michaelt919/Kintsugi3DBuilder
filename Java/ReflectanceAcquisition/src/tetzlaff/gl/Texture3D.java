package tetzlaff.gl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import tetzlaff.helpers.ZipWrapper;

/**
 * An interface for a three-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture3D<ContextType extends Context<ContextType>> extends Texture<ContextType>
{
	/**
	 * Gets the width of the texture.
	 * @return The width of the texture.
	 */
	int getWidth();
	
	/**
	 * Gets the height of the texture.
	 * @return The height of the texture.
	 */
	int getHeight();
	
	/**
	 * Gets the depth of the texture (or for 2D texture arrays, the number of 2D textures in the array).
	 * @return The depth of the texture.
	 */
	int getDepth();
	
	/**
	 * Loads a layer of the texture from an arbitrary input stream.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param fileStream An input stream containing the image in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical ais.
	 * @throws IOException Upon an unrecoverable File I/O problem.
	 */
	void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical) throws IOException;
	
	/**
	 * Loads a layer of the texture from a ZIP file.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param zipFile Represents a location within a ZIP file containing the image in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical ais.
	 * @throws IOException Upon an unrecoverable File I/O problem.
	 */
	void loadLayer(int layerIndex, ZipWrapper zipFile, boolean flipVertical) throws IOException;
	
	/**
	 * Loads a layer of the texture from an ordinary file.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param file A file containing the image in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical ais.
	 * @throws IOException Upon an unrecoverable File I/O problem.
	 */
	void loadLayer(int layerIndex, File file, boolean flipVertical) throws IOException;
	
	/**
	 * Loads a layer of the texture from an arbitrary input stream along with a separate stream containing an alpha mask.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
	 * @param maskStream An input stream containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical ais.
	 * @throws IOException Upon an unrecoverable File I/O problem.
	 */
	void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;
	
	/**
	 * Loads a layer of the texture from a ZIP file along with a separate alpha mask.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param imageZip Represents a location within a ZIP file containing the image in a format supported by Java's ImageIO library.
	 * @param maskZip Represents a location within a ZIP file containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical ais.
	 * @throws IOException Upon an unrecoverable File I/O problem.
	 */
	void loadLayer(int layerIndex, ZipWrapper imageZip, ZipWrapper maskZip, boolean flipVertical) throws IOException;
	
	/**
	 * Loads a layer of the texture from an ordinary file along with an alpha mask in a separate file.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param imageFile A file containing the image in a format supported by Java's ImageIO library.
	 * @param maskFile A file containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical ais.
	 * @throws IOException Upon an unrecoverable File I/O problem.
	 */
	void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical) throws IOException;
	
	/**
	 * Requests that mipmaps be automatically regenerated before this texture is read from again.
	 */
	void generateMipmaps();
	
	/**
	 * Retrieves a layer of the texture for use as an attachment of a framebuffer object.
	 * @param layerIndex The index of the layer to be used.
	 * @return An attachment object which can be attached to a compatible framebuffer object.
	 */
	FramebufferAttachment<ContextType> getLayerAsFramebufferAttachment(int layerIndex);
}
