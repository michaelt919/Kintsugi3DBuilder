package tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.*;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.mvc.controllers.CameraController;
import tetzlaff.mvc.controllers.LightController;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;

public class DragToolControler implements CameraController, CursorPositionListener, MouseButtonPressListener, ScrollListener, LightController {

    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset) {

    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {

    }

    @Override
    public void addAsWindowListener(Window<?> window) {

    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {

    }

    @Override
    public ReadonlyLightModel getLightModel() {
        return null;
    }

    @Override
    public ReadonlyCameraModel getCameraModel() {
        return null;
    }
}