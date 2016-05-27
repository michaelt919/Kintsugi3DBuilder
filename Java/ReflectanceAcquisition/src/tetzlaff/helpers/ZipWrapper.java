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
package tetzlaff.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An abstraction for loading files from a directory that may or may not be zipped.
 * @author Michael Tetzlaff
 *
 */
public class ZipWrapper
{
	/**
	 * The zip file being read from in this zip wrapper.
	 * If reading from a real directory, this will be null.
	 */
	private ZipFile myZip;
	
	/**
	 * The most recently used input stream for reading from a file.
	 */
	private InputStream input;

	/**
	 * Creates a new zip wrapper around a particular file path.
	 * @param file The file path around which to create the zip wrapper, 
	 * which may include a zip file that has been replaced by a non-existent directory of the same name, without the .zip extension.
	 * Zip files within zip files are not supported.
	 * For instance, the file or directory "foo/bar" within the zip file "user/myarchive.zip" would be specified using the (non-existent) file path user/myarchive/foo/bar
	 * @throws IOException Thrown if any File I/O errors occur.
	 */
	public ZipWrapper(File file) throws IOException
	{
		myZip = null;
		input = null;
		
		retrieveFile(file);
	}
	
	/**
	 * Gets whether or not a file exists within this zip wrapper.
	 * @param file The file path of the file to be queried,
	 * which may include a zip file that has been replaced by a non-existent directory of the same name, without the .zip extension.
	 * Zip files within zip files are not supported.
	 * For instance, the file or directory "foo/bar" within the zip file "user/myarchive.zip" would be specified using the (non-existent) file path user/myarchive/foo/bar
	 * @return true if the file exists, false otherwise.
	 */
	public boolean exists(File file)
	{
		// Only works for normal files
		if(file.exists()) return true;

		// Might be in a zip file so try to retrieve it
		try
		{
			InputStream stream = retrieveFile(file);
			return (stream != null);
		}
		catch(IOException e)
		{
			return false;
		}
	}
	
	/**
	 * Retrieves a file from this zip wrapper as a stream.
	 * @param The file path of the file to be queried,
	 * which may include a zip file that has been replaced by a non-existent directory of the same name, without the .zip extension.
	 * Zip files within zip files are not supported.
	 * For instance, the file or directory "foo/bar" within the zip file "user/myarchive.zip" would be specified using the (non-existent) file path user/myarchive/foo/bar
	 * @return An input stream for reading the file.
	 * @throws IOException Thrown if any File I/O errors occur.
	 */
	public InputStream retrieveFile(File file) throws IOException
	{
		try
		{
			input = new FileInputStream(file);
		}
		catch(java.io.FileNotFoundException e)
		{
			String entryNameShort = file.getPath(); entryNameShort = entryNameShort.replace("\\",  "/");
			String zipName = findBaseZipPath(entryNameShort);
			entryNameShort = entryNameShort.replace(zipName + "/", "");
			String entryNameLong = zipName.substring(zipName.lastIndexOf("/")+1) + "/" + entryNameShort;
			zipName += ".zip";
			
			ZipEntry myZipEntry;
			
			// Open the most probable zip and look for the file in the two valid locations
			try
			{
				if(myZip == null) myZip = new ZipFile(zipName);
				myZipEntry = myZip.getEntry(entryNameShort);
				if(myZipEntry == null) myZipEntry = myZip.getEntry(entryNameLong);
			}
			catch(IOException e2)
			{
				// just clear things out and fallthrough
				myZipEntry = null;
			}

			// Did we find it?
			if(myZipEntry != null)
			{
				input = myZip.getInputStream(myZipEntry);
			}			
			else
			{
				input = null;
//				System.err.printf("File not found in folder or zip (%s, %s).\n",
//						file.getPath(), zipName);
			}
		}
		
		return input;
	}
	
	/**
	 * Gets the stream for the file most recently retrieved from this zip wrapper.
	 * @return The input stream.
	 */
	public InputStream getInputStream() { return input; }
	
	/**
	 * Closes any open input streams.
	 * Attempts to use this zip wrapper after calling this method will have undefined results.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		if(input != null) input.close();
		if(myZip != null) myZip.close();
	}
	
	/**
	 * Searches for a zip file in an imaginary file path within which a real zip file has been converted into a non-existent directory.
	 * @param fullPath The imaginary file path for which a zip file needs to be found.
	 * @return The file path to the parent directory in the imaginary file path which actually represents the zip file.
	 * The file path will be returned as a directory path, without a .zip extension.
	 */
	private String findBaseZipPath(String fullPath)
	{
		StringTokenizer tok = new StringTokenizer(fullPath, "/");
		
		String path;
		if (fullPath.startsWith("/"))
		{
			path = "";
		}
		else
		{
			// Drive letter for Windows
			path = tok.nextToken();
		}
		
		boolean found = false;
		while(tok.hasMoreTokens() && !found)
		{
			path += "/" + tok.nextToken();
			File test = new File(path + ".zip");
			found = test.exists();
		}

		return (path);
	}
}
