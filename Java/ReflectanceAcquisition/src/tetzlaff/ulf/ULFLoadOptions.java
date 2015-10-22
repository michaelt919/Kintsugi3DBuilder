package tetzlaff.ulf;

/**
 * A model of the available options when loading an unstructured light field.
 * @author Michael Tetzlaff
 *
 */
public class ULFLoadOptions 
{
	/**
	 * The options to use when loading color images.
	 */
	private ViewSetImageOptions imageOptions;
	
	/**
	 * Whether or not to generate depth images for visibility testing.
	 */
	private boolean depthImagesRequested;
	
	/**
	 * The width of the depth images to be generated (if requested).
	 */
	private int depthImageWidth;
	
	/**
	 * The height of the depth images to be generated (if requested).
	 */
	private int depthImageHeight;
	
	/**
	 * Creates a new object for modeling unstructured light field loading options.
	 * @param imageOptions The options to use when loading color images.
	 * @param depthImagesRequested Whether or not to generate depth images for visibility testing.
	 * @param depthImageWidth  The width of the depth images to be generated (if requested).
	 * @param depthImageHeight The height of the depth images to be generated (if requested).
	 */
	public ULFLoadOptions(ViewSetImageOptions imageOptions, boolean depthImagesRequested, int depthImageWidth, int depthImageHeight) 
	{
		this.imageOptions = imageOptions;
		this.depthImagesRequested = depthImagesRequested;
		this.depthImageWidth = depthImageWidth;
		this.depthImageHeight = depthImageHeight;
	}
	
	/**
	 * Gets the options to use when loading color images.
	 * @return The image loading options.
	 */
	public ViewSetImageOptions getImageOptions() 
	{
		return this.imageOptions;
	}

	/**
	 * Sets the options to use when loading color images.
	 * @param imageOptions The image loading options.
	 */
	public void setImageOptions(ViewSetImageOptions imageOptions) 
	{
		this.imageOptions = imageOptions;
	}
	
	/**
	 * Gets whether or not to generate depth images for visibility testing.
	 * @return true if depth images should be generated, false otherwise.
	 */
	public boolean areDepthImagesRequested()
	{
		return this.depthImagesRequested;
	}

	/**
	 * Sets whether or not to generate depth images for visibility testing.
	 * @param depthImagesRequested true if depth images should be generated, false otherwise.
	 */
	public void setDepthImagesRequested(boolean depthImagesRequested) 
	{
		this.depthImagesRequested = depthImagesRequested;
	}

	/**
	 * Gets the width of the depth images to be generated (if requested).
	 * @return The width of each depth image.
	 */
	public int getDepthImageWidth() 
	{
		return this.depthImageWidth;
	}

	/**
	 * Sets the width of the depth images to be generated (if requested).
	 * @param depthImageWidth The width of each depth image.
	 */
	public void setDepthImageWidth(int depthImageWidth) 
	{
		this.depthImageWidth = depthImageWidth;
	}

	/**
	 * Gets the height of the depth images to be generated (if requested).
	 * @return The height of each depth image.
	 */
	public int getDepthImageHeight() 
	{
		return this.depthImageHeight;
	}

	/**
	 * Sets the height of the depth images to be generated (if requested).
	 * @param depthImageHeight The height of each depth image.
	 */
	public void setDepthImageHeight(int depthImageHeight) 
	{
		this.depthImageHeight = depthImageHeight;
	}
}
