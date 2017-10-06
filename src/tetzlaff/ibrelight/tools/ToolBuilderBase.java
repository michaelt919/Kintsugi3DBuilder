package tetzlaff.ibrelight.tools;

import tetzlaff.models.*;

abstract class ToolBuilderBase<ToolType> implements ToolBuilder<ToolType>
{
    private ToolBindingModel toolBindingModel;
    private ExtendedCameraModel cameraModel;
    private EnvironmentModel environmentModel;
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
    public ToolBuilder<ToolType> setEnvironmentMapModel(EnvironmentModel environmentModel)
    {
        this.environmentModel = environmentModel;
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

    EnvironmentModel getEnvironmentModel()
    {
        return environmentModel;
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
