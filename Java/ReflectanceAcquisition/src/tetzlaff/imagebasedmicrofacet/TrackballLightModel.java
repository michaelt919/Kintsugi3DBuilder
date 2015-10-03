package tetzlaff.imagebasedmicrofacet;

import tetzlaff.gl.helpers.Vector3;

public interface TrackballLightModel
{
	int getActiveTrackball();
	void setActiveTrackball(int index);

	Vector3 getActiveLightColor();
	void setActiveLightColor(Vector3 color);

}
