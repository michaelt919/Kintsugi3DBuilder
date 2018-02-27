package tetzlaff.ibrelight.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

import tetzlaff.util.AbstractImage;

public class LoadingModel 
{
    private LoadingHandler handler;
    private LoadingMonitor loadingMonitor;
    private ReadonlyLoadOptionsModel loadOptionsModel;

    public LoadingMonitor getLoadingMonitor()
    {
        return loadingMonitor;
    }

    public void setLoadingHandler(LoadingHandler handler)
    {
        this.handler = handler;

        if (this.loadingMonitor != null)
        {
            this.handler.setLoadingMonitor(loadingMonitor);
        }
    }

    public void setLoadingMonitor(LoadingMonitor monitor)
    {
        this.loadingMonitor = monitor;

        if (this.handler != null)
        {
            this.handler.setLoadingMonitor(monitor);
        }
    }

    public void setLoadOptionsModel(ReadonlyLoadOptionsModel loadOptionsModel)
    {
        this.loadOptionsModel = loadOptionsModel;
    }

    public void loadFromVSETFile(String id, File vsetFile) throws FileNotFoundException
    {
        this.handler.loadFromVSETFile(id, vsetFile, loadOptionsModel);
    }

    public void loadFromAgisoftFiles(String id, File xmlFile, File meshFile, File undistortedImageDirectory, String primaryViewName)
        throws FileNotFoundException
    {
        this.handler.loadFromAgisoftXMLFile(id, xmlFile, meshFile, undistortedImageDirectory, primaryViewName, loadOptionsModel);
    }

    public Optional<AbstractImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        return this.handler.loadEnvironmentMap(environmentMapFile);
    }

    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        this.handler.loadBackplate(backplateFile);
    }

    public void saveToVSETFile(File vsetFile) throws IOException
    {
        this.handler.saveToVSETFile(vsetFile);
    }

    public DoubleUnaryOperator getLuminanceEncodingFunction()
    {
        return this.handler.getLuminanceEncodingFunction();
    }

    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.handler.setTonemapping(linearLuminanceValues, encodedLuminanceValues);
    }

    public void unload()
    {
        this.handler.unload();
    }
}
