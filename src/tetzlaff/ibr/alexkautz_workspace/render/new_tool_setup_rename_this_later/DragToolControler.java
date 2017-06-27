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

    private final GlobalController globalController;//just to see if enabled
    private float startX;
    private float startY;
    private float mouseScale;

    private boolean enabled(){
        return globalController.getTool() == Tool.DRAG;
    }

    public DragToolControler(GlobalController globalController, LightModelX lightModelX, CameraModelX cameraModelX, Integer mouseButtonOneIndex, Integer mouseButtonTwoIndex) {
        this.globalController = globalController;
        this.lightModelX = lightModelX;
        this.cameraModelX = cameraModelX;
        this.mouseButtonOneIndex = mouseButtonOneIndex;
        this.mouseButtonTwoIndex = mouseButtonTwoIndex;
    }

    private final LightModelX lightModelX;
    private final CameraModelX cameraModelX;

    private final Integer mouseButtonOneIndex;
    private final Integer mouseButtonTwoIndex;














    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset) {
        if(enabled()){
            cameraModelX.setLogRadius(cameraModelX.getLogRadius() + (float) yoffset);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {
        if (enabled())
        {
            if (this.mouseButtonOneIndex >= 0 && window.getMouseButtonState(mouseButtonOneIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale) && (xpos != this.startX || ypos != this.startY))
                {
                    Vector3 rotationVector =
                            new Vector3(
                                    (float)(ypos - this.startY),
                                    (float)(xpos - this.startX),
                                    0.0f
                            );

                    this.cameraModelX.setOrbitMatrix(
                            Matrix4.rotateAxis(
                                    rotationVector.normalized(),
                                    this.mouseScale * rotationVector.length()
                            )
                                    .times(cameraModelX.getOrbitMatrix()));
                }
            }
            else if (this.mouseButtonTwoIndex >= 0 && window.getMouseButtonState(mouseButtonTwoIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale))
                {
                    this.cameraModelX.setOrbitMatrix(
                            Matrix4.rotateZ(this.mouseScale * (xpos - this.startX))
                                    .times(cameraModelX.getOrbitMatrix()));

                    this.cameraModelX.setLogRadius(cameraModelX.getLogRadius() + this.mouseScale * (float)(ypos - this.startY));
                }
            }
        }
    }

    @Override
    public void addAsWindowListener(Window<?> window) {
        window.addCursorPositionListener(this);
        window.addMouseButtonPressListener(this);
        window.addScrollListener(this);
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        System.out.println("Click! Mouse Button index: " + buttonIndex);
        if (enabled() && (buttonIndex == this.mouseButtonOneIndex || buttonIndex == this.mouseButtonTwoIndex))
        {
            CursorPosition pos = window.getCursorPosition();
            WindowSize size = window.getWindowSize();
            this.startX = (float)pos.x;
            this.startY = (float)pos.y;
            this.mouseScale = (float)Math.PI / Math.min(size.width, size.height);
        }
    }

    @Override
    public ReadonlyCameraModel getCameraModel() {
        return cameraModelX;
    }

    @Override
    public ReadonlyLightModel getLightModel() {
        return lightModelX;
    }
}
