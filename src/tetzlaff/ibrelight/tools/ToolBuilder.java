package tetzlaff.ibrelight.tools;

import tetzlaff.models.*;

interface ToolBuilder<ToolType>
{
    ToolBuilder<ToolType> setToolBindingModel(ToolBindingModel toolBindingModel);
    ToolBuilder<ToolType> setCameraModel(ExtendedCameraModel cameraModel);
    ToolBuilder<ToolType> setEnvironmentMapModel(EnvironmentModel environmentModel);
    ToolBuilder<ToolType> setLightingModel(ExtendedLightingModel lightingModel);
    ToolBuilder<ToolType> setObjectModel(ExtendedObjectModel lightingModel);
    ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel);
    ToolBuilder<ToolType> setSettingsModel(SettingsModel settingsModel);
    ToolType build();
}
