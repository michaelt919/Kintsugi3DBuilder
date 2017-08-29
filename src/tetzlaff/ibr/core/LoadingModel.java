package tetzlaff.ibr.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LoadingModel 
{
    private LoadingHandler handler;
    private LoadingMonitor monitor;
    private ReadonlyLoadOptionsModel options;

    public LoadingMonitor getLoadingMonitor()
    {
        return monitor;
    }

    public void setLoadingHandler(LoadingHandler handler)
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

    public void setLoadOptionsModel(ReadonlyLoadOptionsModel options)
    {
        this.options = options;
    }

    public void loadFromVSETFile(String id, File vsetFile) throws FileNotFoundException
    {
        this.handler.loadFromVSETFile(id, vsetFile, options);
    }

    public void loadFromAgisoftFiles(String id, File xmlFile, File meshFile, File undistortedImageDirectory) throws FileNotFoundException
    {
        this.handler.loadFromAgisoftXMLFile(id, xmlFile, meshFile, undistortedImageDirectory, options);
    }

    public void loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        this.handler.loadEnvironmentMap(environmentMapFile);
    }

    public void saveToVSETFile(File vsetFile) throws IOException
    {
        this.handler.saveToVSETFile(vsetFile);
    }

    public void unload()
    {
        this.handler.unload();
    }
}
