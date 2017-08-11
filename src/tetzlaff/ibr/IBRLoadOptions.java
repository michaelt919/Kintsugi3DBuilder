package tetzlaff.ibr;

public class IBRLoadOptions implements tetzlaff.ibr.rendering2.IBRLoadOptions
{
	private boolean colorImagesRequested;
	private boolean mipmapsRequested;
	private boolean compressionRequested;
	private boolean depthImagesRequested;
	private int depthImageWidth;
	private int depthImageHeight;
	
	public IBRLoadOptions() 
	{
	}

	public boolean areColorImagesRequested()
	{
		return this.colorImagesRequested;
	}

	public IBRLoadOptions setColorImagesRequested(boolean colorImagesRequested)
	{
		this.colorImagesRequested = colorImagesRequested;
		return this;
	}

	public boolean areMipmapsRequested() 
	{
		return this.mipmapsRequested;
	}

	public IBRLoadOptions setMipmapsRequested(boolean mipmapsRequested)
	{
		this.mipmapsRequested = mipmapsRequested;
		return this;
	}

	public boolean isCompressionRequested() 
	{
		return this.compressionRequested;
	}

	public IBRLoadOptions setCompressionRequested(boolean compressionRequested)
	{
		this.compressionRequested = compressionRequested;
		return this;
	}

	public boolean areDepthImagesRequested()
	{
		return this.depthImagesRequested;
	}

	public IBRLoadOptions setDepthImagesRequested(boolean depthImagesRequested)
	{
		this.depthImagesRequested = depthImagesRequested;
		return this;
	}

	public int getDepthImageWidth() 
	{
		return this.depthImageWidth;
	}

	public IBRLoadOptions setDepthImageWidth(int depthImageWidth)
	{
		this.depthImageWidth = depthImageWidth;
		return this;
	}

	public int getDepthImageHeight() 
	{
		return this.depthImageHeight;
	}

	public IBRLoadOptions setDepthImageHeight(int depthImageHeight)
	{
		this.depthImageHeight = depthImageHeight;
		return this;
	}
}
