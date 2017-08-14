package tetzlaff.ibr.javafx.models;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.rendering2.ToolModelImp;

public class JavaFXModels 
{
    private final JavaFXCameraModel cameraModel;
    private final JavaFXEnvironmentMapModel environmentMapModel;
    private final JavaFXLightingModel lightModel;
    private final JavaFXLoadOptionsModel loadOptionsModel;
    private final JavaFXSettingsModel settingsModel;
    private final ToolModelImp toolModel;
    
    private static JavaFXModels instance = new JavaFXModels();
    
    public static JavaFXModels getInstance()
    {
    	return instance;
    }

    private JavaFXModels()
    {
        cameraModel = new JavaFXCameraModel();
        toolModel = new ToolModelImp();
        environmentMapModel = new JavaFXEnvironmentMapModel(toolModel);
        lightModel = new JavaFXLightingModel(environmentMapModel);
        loadOptionsModel = new JavaFXLoadOptionsModel();
        settingsModel = new JavaFXSettingsModel();
    }

    public JavaFXCameraModel getCameraModel()
    {
        return cameraModel;
    }

    public JavaFXLightingModel getLightModel()
    {
        return lightModel;
    }

    public ToolModelImp getToolModel()
    {
        return toolModel;
    }

    public JavaFXEnvironmentMapModel getEnvironmentMapModel()
    {
        return environmentMapModel;
    }

	public JavaFXLoadOptionsModel getLoadOptionsModel() 
	{
		return loadOptionsModel;
	}

	public JavaFXSettingsModel getSettingsModel()
	{
		return settingsModel;
	}
}
