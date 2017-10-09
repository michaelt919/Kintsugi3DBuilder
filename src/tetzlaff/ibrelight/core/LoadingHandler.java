package tetzlaff.ibrelight.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface LoadingHandler 
{
    void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions) throws FileNotFoundException;
    void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, ReadonlyLoadOptionsModel loadOptions)
        throws FileNotFoundException;

    void loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException;
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    void saveToVSETFile(File vsetFile) throws IOException;

    void unload();

    void setLoadingMonitor(LoadingMonitor loadingMonitor);
}
