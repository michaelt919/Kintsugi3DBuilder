package tetzlaff.mvc.controllers;

import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.window.Window;

public interface LightController 
{
	void addAsWindowListener(Window window);
	ReadonlyLightModel getModel();
}
