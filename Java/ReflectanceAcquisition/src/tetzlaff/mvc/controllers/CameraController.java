package tetzlaff.mvc.controllers;

import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.window.Window;

public interface CameraController 
{
	void addAsWindowListener(Window window);
	ReadonlyCameraModel getModel();
}
