package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

public interface IBRLoadingHandler 
{
	void loadFromVSETFile(String id, File vsetFile, ReadonlyIBRLoadOptionsModel loadOptions) throws IOException;
	void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, ReadonlyIBRLoadOptionsModel loadOptions) throws IOException;
	
	void loadEnvironmentMap(File environmentMapFile) throws IOException;
	
	void setLoadingMonitor(LoadingMonitor loadingMonitor);
}
