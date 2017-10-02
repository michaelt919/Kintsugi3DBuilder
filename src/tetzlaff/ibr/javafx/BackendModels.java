package tetzlaff.ibr.javafx;

import tetzlaff.ibr.javafx.backend.*;

public final class BackendModels
{
    private static final BackendModels INSTANCE = new BackendModels();

    static BackendModels getInstance()
    {
        return INSTANCE;
    }

    private final JavaFXCameraModel cameraModel;
    private final JavaFXEnvironmentModel environmentMapModel;
    private final JavaFXLightingModel lightingModel;
    private final JavaFXObjectModel objectModel;
    private final JavaFXLoadOptionsModel loadOptionsModel;
    private final JavaFXSettingsModel settingsModel;

    private BackendModels()
    {
        cameraModel = new JavaFXCameraModel();
        environmentMapModel = new JavaFXEnvironmentModel();
        objectModel = new JavaFXObjectModel();
        lightingModel = new JavaFXLightingModel(environmentMapModel);
        loadOptionsModel = new JavaFXLoadOptionsModel();
        settingsModel = new JavaFXSettingsModel();
    }

    public JavaFXCameraModel getCameraModel()
    {
        return cameraModel;
    }

    public JavaFXLightingModel getLightingModel()
    {
        return lightingModel;
    }

    public JavaFXObjectModel getObjectModel()
    {
        return objectModel;
    }

    public JavaFXLoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    public JavaFXSettingsModel getSettingsModel()
    {
        return settingsModel;
    }

    public JavaFXEnvironmentModel getEnvironmentMapModel()
    {
        return environmentMapModel;
    }
}
