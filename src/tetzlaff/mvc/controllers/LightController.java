package tetzlaff.mvc.controllers;

import tetzlaff.gl.window.Window;
import tetzlaff.mvc.models.ReadonlyLightingModel;

public interface LightController 
{
	void addAsWindowListener(Window<?> window);
	ReadonlyLightingModel getLightingModel();
}
