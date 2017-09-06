package tetzlaff.ibr.tools;

import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.models.*;

abstract class ToolBuilderBase<ToolType> implements ToolBuilder<ToolType>
{
    private ToolBindingModel toolBindingModel;
    private ExtendedCameraModel cameraModel;
    private EnvironmentMapModel environmentMapModel;
    private ExtendedLightingModel lightingModel;
    private ExtendedObjectModel objectModel;
    private SceneViewportModel sceneViewportModel;
    private SettingsModel settingsModel;

    protected ToolBuilderBase()
    {
    }

    @Override
    public ToolBuilder<ToolType> setToolBindingModel(ToolBindingModel toolBindingModel)
    {
        this.toolBindingModel = toolBindingModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setCameraModel(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setEnvironmentMapModel(EnvironmentMapModel environmentMapModel)
    {
        this.environmentMapModel = environmentMapModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setLightingModel(ExtendedLightingModel lightingModel)
    {
        this.lightingModel = lightingModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setObjectModel(ExtendedObjectModel objectModel)
    {
        this.objectModel = objectModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel)
    {
        this.sceneViewportModel = sceneViewportModel;
        return this;
    }

    @Override
    public ToolBuilder<ToolType> setSettingsModel(SettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
        return this;
    }

    ToolBindingModel getToolBindingModel()
    {
        return toolBindingModel;
    }

    ExtendedCameraModel getCameraModel()
    {
        return cameraModel;
    }

    EnvironmentMapModel getEnvironmentMapModel()
    {
        return environmentMapModel;
    }

    ExtendedLightingModel getLightingModel()
    {
        return lightingModel;
    }

    ExtendedObjectModel getObjectModel()
    {
        return objectModel;
    }

    SceneViewportModel getSceneViewportModel()
    {
        return sceneViewportModel;
    }

    SettingsModel getSettingsModel()
    {
        return settingsModel;
    }
}
