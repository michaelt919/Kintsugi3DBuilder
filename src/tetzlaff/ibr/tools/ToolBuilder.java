package tetzlaff.ibr.tools;

import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.SceneViewportModel;

interface ToolBuilder<ToolType>
{
    ToolBuilder<ToolType> setToolSelectionModel(ToolBindingModel toolBindingModel);
    ToolBuilder<ToolType> setCameraModel(ExtendedCameraModel cameraModel);
    ToolBuilder<ToolType> setEnvironmentMapModel(ReadonlyEnvironmentMapModel environmentMapModel);
    ToolBuilder<ToolType> setLightingModel(ExtendedLightingModel lightingModel);
    ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel);
    ToolType build();
}
