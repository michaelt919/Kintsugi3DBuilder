package tetzlaff.ibr.core;

import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.ReadonlyLightingModel;

public interface IBRelightModelAccess
{
    ReadonlyCameraModel getCameraModel();
    ReadonlyLightingModel getLightingModel();
    ReadonlyLoadOptionsModel getLoadOptionsModel();
    ReadonlySettingsModel getSettingsModel();
}
