package tetzlaff.ibr.javafx;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.core.IBRelightModelAccess;
import tetzlaff.ibr.core.LoadingModel;
import tetzlaff.ibr.core.ReadonlyLoadOptionsModel;
import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.ibr.javafx.models.MultithreadCameraModel;
import tetzlaff.ibr.javafx.models.MultithreadLightingModel;
import tetzlaff.ibr.javafx.models.MultithreadObjectModel;
import tetzlaff.ibr.tools.ToolBindingModel;
import tetzlaff.ibr.tools.ToolBindingModelImpl;
import tetzlaff.models.EnvironmentModel;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.ExtendedObjectModel;

public final class Models implements IBRelightModelAccess
{
    private final ExtendedCameraModel cameraModel;
    private final EnvironmentModel environmentModel;
    private final ExtendedLightingModel lightingModel;
    private final ExtendedObjectModel objectModel;
    private final ReadonlyLoadOptionsModel loadOptionsModel;
    private final SettingsModel settingsModel;
    private final ToolBindingModel toolModel;
    private final LoadingModel loadingModel;

    private static final Models INSTANCE = new Models();

    public static Models getInstance()
    {
        return INSTANCE;
    }

    private Models()
    {
        cameraModel = new MultithreadCameraModel(BackendModels.getInstance().getCameraModel());
        toolModel = new ToolBindingModelImpl();
        objectModel = new MultithreadObjectModel(BackendModels.getInstance().getObjectModel());
        lightingModel = new MultithreadLightingModel(BackendModels.getInstance().getLightingModel());
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
