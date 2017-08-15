package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

public interface LoadingHandler 
{
	void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions) throws IOException;
	void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, ReadonlyLoadOptionsModel loadOptions) throws IOException;
	
	void loadEnvironmentMap(File environmentMapFile) throws IOException;
	
	void setLoadingMonitor(LoadingMonitor loadingMonitor);
}
