package tetzlaff.ulf;

import tetzlaff.gl.helpers.Drawable;

public interface ULFDrawable extends Drawable
{
	UnstructuredLightField getLightField();
	void setOnLoadCallback(ULFLoadedCallback callback);
}
