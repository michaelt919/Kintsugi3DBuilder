package tetzlaff.mvc.controllers;

import tetzlaff.gl.window.Window;
import tetzlaff.mvc.models.ReadonlyCameraModel;

public interface CameraController 
{
	void addAsWindowListener(Window<?> window);
	ReadonlyCameraModel getCameraModel();
}
