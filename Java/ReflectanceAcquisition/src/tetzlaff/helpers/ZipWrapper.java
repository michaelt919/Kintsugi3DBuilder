package tetzlaff.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipWrapper
{
	private ZipFile myZip;
	private ZipEntry myZipEntry;
	private InputStream input;

	public ZipWrapper(File file) throws IOException
	{
		myZip = null;
		myZipEntry = null;
		input = null;
		
		retrieveFile(file);
	}
	
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
	
	public InputStream getInputStream() { return input; }
	
	public void close() throws IOException
	{
		if(input != null) input.close();
		if(myZip != null) myZip.close();
	}
	
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
