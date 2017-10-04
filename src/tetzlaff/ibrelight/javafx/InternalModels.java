package tetzlaff.ibrelight.javafx;

import tetzlaff.ibrelight.javafx.internal.*;

public final class InternalModels
{
    private static final InternalModels INSTANCE = new InternalModels();

    static InternalModels getInstance()
    {
        return INSTANCE;
    }

    private final CameraModelImpl cameraModel;
    private final EnvironmentModelImpl environmentModel;
    private final LightingModelImpl lightingModel;
    private final ObjectModelImpl objectModel;
    private final LoadOptionsModelImpl loadOptionsModel;
    private final SettingsModelImpl settingsModel;

    private InternalModels()
    {
        cameraModel = new CameraModelImpl();
        environmentModel = new EnvironmentModelImpl();
        objectModel = new ObjectModelImpl();
        lightingModel = new LightingModelImpl(environmentModel);
        loadOptionsModel = new LoadOptionsModelImpl();
        settingsModel = new SettingsModelImpl();
    }

    public CameraModelImpl getCameraModel()
    {
        return cameraModel;
    }

    public LightingModelImpl getLightingModel()
    {
        return lightingModel;
    }

    public ObjectModelImpl getObjectModel()
    {
        return objectModel;
    }

    public LoadOptionsModelImpl getLoadOptionsModel()
    {
        return loadOptionsModel;
    }

    public SettingsModelImpl getSettingsModel()
    {
        return settingsModel;
    }

    public EnvironmentModelImpl getEnvironmentModel()
    {
        return environmentModel;
    }
}
