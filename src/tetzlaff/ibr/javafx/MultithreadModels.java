package tetzlaff.ibr.javafx;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.core.IBRelightModelAccess;
import tetzlaff.ibr.core.LoadingModel;
import tetzlaff.ibr.core.ReadonlyLoadOptionsModel;
import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.ibr.javafx.multithread.*;
import tetzlaff.ibr.tools.ToolBindingModel;
import tetzlaff.ibr.tools.ToolBindingModelImpl;
import tetzlaff.models.EnvironmentModel;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.ExtendedObjectModel;

public final class MultithreadModels implements IBRelightModelAccess
{
    private final ExtendedCameraModel cameraModel;
    private final EnvironmentModel environmentModel;
    private final ExtendedLightingModel lightingModel;
    private final ExtendedObjectModel objectModel;
    private final SettingsModel settingsModel;
    private final ReadonlyLoadOptionsModel loadOptionsModel;
    private final ToolBindingModel toolModel;
    private final LoadingModel loadingModel;

    private static final MultithreadModels INSTANCE = new MultithreadModels();

    public static MultithreadModels getInstance()
    {
        return INSTANCE;
    }

    private MultithreadModels()
    {
        cameraModel = new CameraModelWrapper(InternalModels.getInstance().getCameraModel());
        objectModel = new ObjectModelWrapper(InternalModels.getInstance().getObjectModel());
        lightingModel = new LightingModelWrapper(InternalModels.getInstance().getLightingModel());
        environmentModel = new EnvironmentModelWrapper(InternalModels.getInstance().getEnvironmentModel());
        settingsModel = new SettingsModelWrapper(InternalModels.getInstance().getSettingsModel());
        loadOptionsModel = InternalModels.getInstance().getLoadOptionsModel();
        toolModel = new ToolBindingModelImpl();
        loadingModel = new LoadingModel();
        loadingModel.setLoadOptionsModel(loadOptionsModel);
    }

    @Override
    public ExtendedCameraModel getCameraModel()
    {
        return cameraModel;
    }

    @Override
    public ExtendedLightingModel getLightingModel()
    {
        return lightingModel;
    }

    @Override
    public ExtendedObjectModel getObjectModel()
    {
        return objectModel;
    }

    @Override
    public ReadonlyLoadOptionsModel getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    @Override
    public SettingsModel getSettingsModel()
    {
        return settingsModel;
    }

    public ToolBindingModel getToolModel()
    {
        return toolModel;
    }

    public EnvironmentModel getEnvironmentModel()
    {
        return environmentModel;
    }

    public LoadingModel getLoadingModel()
    {
        return loadingModel;
    }
}
