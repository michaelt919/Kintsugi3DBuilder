package tetzlaff.ulf;

import java.io.File;

/**
 * A model of the available options when loading the images for a view set.
 * @author Michael Tetzlaff
 *
 */
public class ViewSetImageOptions 
{
	/**
	 * The absolute file path of the images.
	 */
	private File filePath;
	
	/**
	 * Whether or not images should be automatically loaded.
	 */
	private boolean loadingRequested;
	
	/**
	 * Whether or not mipmaps should be generated for images.
	 */
	private boolean mipmapsRequested;
	
	/**
	 * Whether or not images should be hardware-compressed.
	 */
	private boolean compressionRequested;
	
	/**
	 * Creates a new object for modeling view set image loading options.
	 * @param filePath The absolute file path of the images.
	 * @param loadingRequested Whether or not images should be automatically loaded.
	 * @param mipmapsRequested Whether or not mipmaps should be generated for images.
	 * @param compressionRequested Whether or not images should be hardware-compressed.
	 */
	public ViewSetImageOptions(File filePath, boolean loadingRequested, boolean mipmapsRequested, boolean compressionRequested) 
	{
		this.filePath = filePath;
		this.loadingRequested = loadingRequested;
		this.mipmapsRequested = mipmapsRequested;
		this.compressionRequested = compressionRequested;
	}

	/**
	 * Gets the absolute file path of the images.
	 * @return The absolute file path.
	 */
	public File getFilePath() 
	{
		return this.filePath;
	}

	/**
	 * Sets the absolute file path of the images.
	 * @param filePath The absolute file path.
	 */
	public void setFilePath(File filePath) 
	{
		this.filePath = filePath;
	}

	/**
	 * Gets whether or not images should be automatically loaded.
	 * @return true if images should be loaded, false otherwise.
	 */
	public boolean isLoadingRequested() 
	{
		return this.loadingRequested;
	}

	/**
	 * Sets whether or not images should be automatically loaded.
	 * @param loadingRequested true if images should be loaded, false otherwise.
	 */
	public void setLoadingRequested(boolean loadingRequested) 
	{
		this.loadingRequested = loadingRequested;
	}

	/**
	 * Gets whether or not mipmaps should be generated for images.
	 * @return true if mipmaps should be generated, false otherwise.
	 */
	public boolean areMipmapsRequested() 
	{
		return this.mipmapsRequested;
	}

	/**
	 * Sets whether or not mipmaps should be generated for images.
	 * @param mipmapsRequested true if mipmaps should be generated, false otherwise.
	 */
	public void setMipmapsRequested(boolean mipmapsRequested) 
	{
		this.mipmapsRequested = mipmapsRequested;
	}

	/**
	 * Gets whether or not images should be hardware-compressed.
	 * @return true if images should be compressed, false otherwise.
	 */
	public boolean isCompressionRequested() 
	{
		return this.compressionRequested;
	}

	/**
	 * Sets whether or not images should be hardware-compressed.
	 * @param compressionRequested true if images should be compressed, false otherwise.
	 */
	public void setCompressionRequested(boolean compressionRequested) 
	{
		this.compressionRequested = compressionRequested;
	}
}
