package tetzlaff.ibrelight.core;

import tetzlaff.models.*;

public interface IBRelightModels
{
    ReadonlyCameraModel getCameraModel();
    ReadonlyLightingModel getLightingModel();
    ReadonlyObjectModel getObjectModel();
    ReadonlyEnvironmentModel getEnvironmentModel();
    ReadonlySettingsModel getSettingsModel();
    SceneViewportModel getSceneViewportModel();
    ReadonlyLoadOptionsModel getLoadOptionsModel();
    LoadingModel getLoadingModel();
}
