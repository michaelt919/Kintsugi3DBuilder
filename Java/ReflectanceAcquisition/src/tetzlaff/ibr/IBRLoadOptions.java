package tetzlaff.ibr;


public class IBRLoadOptions 
{
	private ViewSetImageOptions imageOptions;
	private boolean depthImagesRequested;
	private int depthImageWidth;
	private int depthImageHeight;
	
	public IBRLoadOptions(ViewSetImageOptions imageOptions, boolean depthImagesRequested, int depthImageWidth, int depthImageHeight) 
	{
		this.imageOptions = imageOptions;
		this.depthImagesRequested = depthImagesRequested;
		this.depthImageWidth = depthImageWidth;
		this.depthImageHeight = depthImageHeight;
	}
	
	public ViewSetImageOptions getImageOptions() 
	{
		return this.imageOptions;
	}

	public void setImageOptions(ViewSetImageOptions imageOptions) 
	{
		this.imageOptions = imageOptions;
	}
	

	public boolean areDepthImagesRequested()
	{
		return this.depthImagesRequested;
	}

	public void setDepthImagesRequested(boolean depthImagesRequested) 
	{
		this.depthImagesRequested = depthImagesRequested;
	}

	public int getDepthImageWidth() 
	{
		return this.depthImageWidth;
	}

	public void setDepthImageWidth(int depthImageWidth) 
	{
		this.depthImageWidth = depthImageWidth;
	}

	public int getDepthImageHeight() 
	{
		return this.depthImageHeight;
	}

	public void setDepthImageHeight(int depthImageHeight) 
	{
		this.depthImageHeight = depthImageHeight;
	}
}
