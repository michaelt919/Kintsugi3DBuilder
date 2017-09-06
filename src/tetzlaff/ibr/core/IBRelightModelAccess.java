package tetzlaff.ibr.core;

import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.ReadonlyObjectModel;

public interface IBRelightModelAccess
{
    ReadonlyCameraModel getCameraModel();
    ReadonlyLightingModel getLightingModel();
    ReadonlyObjectModel getObjectModel();
    ReadonlyLoadOptionsModel getLoadOptionsModel();
    ReadonlySettingsModel getSettingsModel();
}
