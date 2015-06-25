package tetzlaff.gl.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.zip.*;

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
			
			if(myZip == null) myZip = new ZipFile(zipName);
			
			myZipEntry = myZip.getEntry(entryNameShort);
			if(myZipEntry == null) myZipEntry = myZip.getEntry(entryNameLong);
			if(myZipEntry != null)
			{
				input = myZip.getInputStream(myZipEntry);
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
		
		String path = "";
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
