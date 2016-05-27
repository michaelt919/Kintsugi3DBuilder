/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
