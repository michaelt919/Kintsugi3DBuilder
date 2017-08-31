package tetzlaff.ibr.tools;

import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.models.EnvironmentMapModel;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.SceneViewportModel;

interface ToolBuilder<ToolType>
{
    ToolBuilder<ToolType> setToolBindingModel(ToolBindingModel toolBindingModel);
    ToolBuilder<ToolType> setCameraModel(ExtendedCameraModel cameraModel);
    ToolBuilder<ToolType> setEnvironmentMapModel(EnvironmentMapModel environmentMapModel);
    ToolBuilder<ToolType> setLightingModel(ExtendedLightingModel lightingModel);
    ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel);
    ToolBuilder<ToolType> setSettingsModel(SettingsModel settingsModel);
    ToolType build();
}
