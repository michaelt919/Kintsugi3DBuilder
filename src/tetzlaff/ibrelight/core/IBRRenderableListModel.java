package tetzlaff.ibrelight.core;

import tetzlaff.gl.core.Context;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.ReadonlyObjectModel;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.SelectableList;

public interface IBRRenderableListModel<ContextType extends Context<ContextType>> extends LoadingHandler, SelectableList<IBRRenderable<ContextType>>
{
    void setSettingsModel(ReadonlySettingsModel settingsModel);
    void setObjectModel(ReadonlyObjectModel objectModel);
    void setCameraModel(ReadonlyCameraModel cameraModel);
    void setLightingModel(ReadonlyLightingModel lightingModel);
}
