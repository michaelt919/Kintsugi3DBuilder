package tetzlaff.ibr;

import javax.swing.ComboBoxModel;

import tetzlaff.gl.Context;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightingModel;
import tetzlaff.mvc.models.ReadonlyObjectModel;

public interface IBRRenderableListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<IBRRenderable<ContextType>>, LoadingHandler
{
	@Override
	IBRRenderable<ContextType> getSelectedItem();

	void setSettingsModel(ReadonlySettingsModel settingsModel);
	void setObjectModel(ReadonlyObjectModel objectModel);
	void setCameraModel(ReadonlyCameraModel cameraModel);
	void setLightingModel(ReadonlyLightingModel lightingModel);
}
