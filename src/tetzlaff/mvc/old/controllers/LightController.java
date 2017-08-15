package tetzlaff.mvc.old.controllers;

import tetzlaff.gl.window.Window;
import tetzlaff.models.ReadonlyLightingModel;

public interface LightController 
{
	void addAsWindowListener(Window<?> window);
	ReadonlyLightingModel getLightingModel();
}
