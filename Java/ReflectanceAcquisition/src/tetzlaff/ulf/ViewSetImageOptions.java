package tetzlaff.ulf;

import java.io.File;

public class ViewSetImageOptions 
{
	private File filePath;
	private boolean loadingRequested;
	private boolean mipmapsRequested;
	private boolean compressionRequested;
	
	public ViewSetImageOptions(File filePath, boolean loadingRequested, boolean mipmapsRequested, boolean compressionRequested) 
	{
		this.filePath = filePath;
		this.loadingRequested = loadingRequested;
		this.mipmapsRequested = mipmapsRequested;
		this.compressionRequested = compressionRequested;
	}

	public File getFilePath() 
	{
		return this.filePath;
	}

	public void setFilePath(File filePath) 
	{
		this.filePath = filePath;
	}

	public boolean isLoadingRequested() 
	{
		return this.loadingRequested;
	}

	public void setLoadingRequested(boolean loadingRequested) 
	{
		this.loadingRequested = loadingRequested;
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
}
