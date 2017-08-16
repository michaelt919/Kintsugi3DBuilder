package tetzlaff.ibr.javafx.models;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.LoadingModel;

public class JavaFXModels
{
    private final JavaFXCameraModel cameraModel;
    private final JavaFXEnvironmentMapModel environmentMapModel;
    private final JavaFXLightingModel lightingModel;
    private final JavaFXLoadOptionsModel loadOptionsModel;
    private final JavaFXSettingsModel settingsModel;
    private final JavaFXToolSelectionModel toolModel;
    private final LoadingModel loadingModel;

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
        lightingModel = new JavaFXLightingModel(environmentMapModel);
        loadOptionsModel = new JavaFXLoadOptionsModel();
        settingsModel = new JavaFXSettingsModel();
        loadingModel = new LoadingModel();
        loadingModel.setLoadOptionsModel(loadOptionsModel);
    }

    public JavaFXCameraModel getCameraModel()
    {
        return cameraModel;
    }

    public JavaFXLightingModel getLightingModel()
    {
        return lightingModel;
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

    public LoadingModel getLoadingModel()
    {
        return loadingModel;
    }
}
