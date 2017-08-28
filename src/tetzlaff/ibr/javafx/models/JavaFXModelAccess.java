package tetzlaff.ibr.javafx.models;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.core.IBRelightModelAccess;
import tetzlaff.ibr.core.LoadingModel;

public class JavaFXModelAccess implements IBRelightModelAccess
{
    private final JavaFXCameraModel cameraModel;
    private final JavaFXEnvironmentMapModel environmentMapModel;
    private final JavaFXLightingModel lightingModel;
    private final JavaFXLoadOptionsModel loadOptionsModel;
    private final JavaFXSettingsModel settingsModel;
    private final JavaFXToolBindingModel toolModel;
    private final LoadingModel loadingModel;

    private static final JavaFXModelAccess instance = new JavaFXModelAccess();

    public static JavaFXModelAccess getInstance()
    {
        return instance;
    }

    private JavaFXModelAccess()
    {
        cameraModel = new JavaFXCameraModel();
        toolModel = new JavaFXToolBindingModel();
        environmentMapModel = new JavaFXEnvironmentMapModel();
        lightingModel = new JavaFXLightingModel(environmentMapModel);
        loadOptionsModel = new JavaFXLoadOptionsModel();
        settingsModel = new JavaFXSettingsModel();
        loadingModel = new LoadingModel();
        loadingModel.setLoadOptionsModel(loadOptionsModel);
    }

    @Override
    public JavaFXCameraModel getCameraModel()
    {
        return cameraModel;
    }

    @Override
    public JavaFXLightingModel getLightingModel()
    {
        return lightingModel;
    }

    @Override
    public JavaFXLoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    @Override
    public JavaFXSettingsModel getSettingsModel()
    {
        return settingsModel;
    }

    public JavaFXToolBindingModel getToolModel()
    {
        return toolModel;
    }

    public JavaFXEnvironmentMapModel getEnvironmentMapModel()
    {
        return environmentMapModel;
    }

    public LoadingModel getLoadingModel()
    {
        return loadingModel;
    }
}
