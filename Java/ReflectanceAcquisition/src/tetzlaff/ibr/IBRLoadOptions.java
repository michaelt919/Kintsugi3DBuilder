package tetzlaff.ibr;

import java.io.File;


public class IBRLoadOptions 
{
	private File imagePathOverride;
	private boolean colorImagesRequested;
	private boolean mipmapsRequested;
	private boolean compressionRequested;
	private boolean depthImagesRequested;
	private int depthImageWidth;
	private int depthImageHeight;
	
	public IBRLoadOptions() 
	{
	}
	
	public File getImagePathOverride() 
	{
		return this.imagePathOverride;
	}

	public void setImagePathOverride(File imagePathOverride) 
	{
		this.imagePathOverride = imagePathOverride;
	}

	public boolean areColorImagesRequested() 
	{
		return this.colorImagesRequested;
	}

	public void setColorImagesRequested(boolean colorImagesRequested) 
	{
		this.colorImagesRequested = colorImagesRequested;
	}

	public boolean areMipmapsRequested() 
	{
		return this.mipmapsRequested;
	}

	public void setMipmapsRequested(boolean mipmapsRequested) 
	{
		this.mipmapsRequested = mipmapsRequested;
	}

	public boolean isCompressionRequested() 
	{
		return this.compressionRequested;
	}

	public void setCompressionRequested(boolean compressionRequested) 
	{
		this.compressionRequested = compressionRequested;
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
