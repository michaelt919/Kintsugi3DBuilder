package tetzlaff.ibr.javafx.models;//Created by alexk on 7/19/2017.


public class JavaFXModels 
{
    private final JavaFXCameraModel cameraModel;
    private final JavaFXEnvironmentMapModel environmentMapModel;
    private final JavaFXLightingModel lightModel;
    private final JavaFXLoadOptionsModel loadOptionsModel;
    private final JavaFXSettingsModel settingsModel;
    private final JavaFXToolSelectionModel toolModel;
    
    private static JavaFXModels instance = new JavaFXModels();
    
    public static JavaFXModels getInstance()
    {
    	return instance;
    }

    private JavaFXModels()
    {
        cameraModel = new JavaFXCameraModel();
        toolModel = new JavaFXToolSelectionModel();
        environmentMapModel = new JavaFXEnvironmentMapModel();
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

    public JavaFXToolSelectionModel getToolModel()
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
