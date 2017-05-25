package tetzlaff.mvc.controllers;

import tetzlaff.gl.window.Window;
import tetzlaff.mvc.models.ReadonlyLightModel;

public interface LightController 
{
	void addAsWindowListener(Window<?> window);
	ReadonlyLightModel getModel();
}
