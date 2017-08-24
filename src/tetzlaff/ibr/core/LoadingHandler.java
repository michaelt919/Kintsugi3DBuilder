package tetzlaff.ibr.core;

import java.io.File;
import java.io.FileNotFoundException;

public interface LoadingHandler 
{
    void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions) throws FileNotFoundException;
    void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, ReadonlyLoadOptionsModel loadOptions)
        throws FileNotFoundException;

    void loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException;

    void setLoadingMonitor(LoadingMonitor loadingMonitor);
}
