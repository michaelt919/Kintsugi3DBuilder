package tetzlaff.mvc.old.controllers;

import tetzlaff.gl.window.Window;
import tetzlaff.models.ReadonlyCameraModel;

public interface CameraController 
{
    void addAsWindowListener(Window<?> window);
    ReadonlyCameraModel getCameraModel();
}
