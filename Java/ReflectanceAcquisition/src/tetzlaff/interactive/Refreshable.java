package tetzlaff.interactive;

import java.io.File;

public interface Refreshable 
{
	void initialize();
	void refresh();
	void terminate();
	void requestScreenshot(String fileFormat, File file);
}
