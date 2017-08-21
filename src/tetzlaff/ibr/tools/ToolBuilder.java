package tetzlaff.ibr.tools;

import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.SceneViewportModel;

interface ToolBuilder<ToolType extends Tool>
{
    ToolBuilder<ToolType> setToolSelectionModel(ToolSelectionModel toolSelectionModel);
    ToolBuilder<ToolType> setCameraModel(ExtendedCameraModel cameraModel);
    ToolBuilder<ToolType> setEnvironmentMapModel(ReadonlyEnvironmentMapModel environmentMapModel);
    ToolBuilder<ToolType> setLightingModel(ReadonlyLightingModel lightingModel);
    ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel);
    ToolType build();
}
