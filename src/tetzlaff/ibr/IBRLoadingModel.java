package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

public class IBRLoadingModel 
{
	private IBRLoadingHandler handler;
	private LoadingMonitor monitor;
	private ReadonlyIBRLoadOptionsModel options;
	
	private static IBRLoadingModel instance = new IBRLoadingModel();
	
	public static IBRLoadingModel getInstance()
	{
		return instance;
	}
	
	private IBRLoadingModel()
	{
	}
	
	public void setLoadingHandler(IBRLoadingHandler handler)
	{
		this.handler = handler;
		
		if (this.monitor != null)
		{
			this.handler.setLoadingMonitor(monitor);
		}
	}
	
	public void setLoadingMonitor(LoadingMonitor monitor)
	{
		this.monitor = monitor;
		
		if (this.handler != null)
		{
			this.handler.setLoadingMonitor(monitor);
		}
	}
	
	public void setLoadOptionsModel(ReadonlyIBRLoadOptionsModel options)
	{
		this.options = options;
	}
	
	public void loadFromVSETFile(String id, File vsetFile) throws IOException
	{
		this.handler.loadFromVSETFile(id, vsetFile, options);
	}
	
	public void loadFromAgisoftFiles(String id, File xmlFile, File meshFile, File undistortedImageDirectory) throws IOException
	{
		this.handler.loadFromAgisoftXMLFile(id, xmlFile, meshFile, undistortedImageDirectory, options);
	}
	
	public void loadEnvironmentMap(File environmentMapFile) throws IOException
	{
		this.handler.loadEnvironmentMap(environmentMapFile);
	}
}
