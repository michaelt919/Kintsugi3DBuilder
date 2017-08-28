package tetzlaff.ibr.tools;

import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.SceneViewportModel;

abstract class ToolBuilderBase<ToolType> implements ToolBuilder<ToolType>
{
    private ToolBindingModel toolBindingModel;
    private ExtendedCameraModel cameraModel;
    private ReadonlyEnvironmentMapModel environmentMapModel;
    private ExtendedLightingModel lightingModel;
    private SceneViewportModel sceneViewportModel;

    protected ToolBuilderBase()
    {
    }

    @Override
    public ToolBuilder<ToolType> setToolSelectionModel(ToolBindingModel toolBindingModel)
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
    public ToolBuilder<ToolType> setEnvironmentMapModel(ReadonlyEnvironmentMapModel environmentMapModel)
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
    public ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel)
    {
        this.sceneViewportModel = sceneViewportModel;
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

    ReadonlyEnvironmentMapModel getEnvironmentMapModel()
    {
        return environmentMapModel;
    }

    ExtendedLightingModel getLightingModel()
    {
        return lightingModel;
    }

    SceneViewportModel getSceneViewportModel()
    {
        return sceneViewportModel;
    }
}
