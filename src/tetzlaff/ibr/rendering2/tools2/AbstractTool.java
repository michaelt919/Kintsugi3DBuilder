package tetzlaff.ibr.rendering2.tools2;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.mvc.models.ControllableCameraModel;
import tetzlaff.mvc.models.ControllableEnvironmentMapModel;
import tetzlaff.mvc.models.ControllableLightModel;

class AbstractTool implements CursorPositionListener, MouseButtonPressListener, ScrollListener, KeyPressListener{

    protected double mouseStartX_MB1;
    protected double mouseStartY_MB1;

    protected static final int MB1 = 0;//mouse button
    protected static final int MB2 = 1;
    protected static final int MB3 = 2;

    protected ControllableCameraModel cameraModel;
    protected ControllableEnvironmentMapModel environmentMapModel;
    protected ControllableLightModel lightModel;

    AbstractTool(ControllableCameraModel cameraModel, ControllableEnvironmentMapModel environmentMapModel, ControllableLightModel lightModel) {
        this.cameraModel = cameraModel;
        this.environmentMapModel = environmentMapModel;
        this.lightModel = lightModel;
    }

    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset) {

    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {

    }

    @Override
    public void keyPressed(Window<?> window, int keycode, ModifierKeys mods) {

    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        System.out.println("MB: " + buttonIndex);
        if(buttonIndex == MB1){
            CursorPosition pos = window.getCursorPosition();
            mouseStartX_MB1 = pos.x;
            mouseStartY_MB1 = pos.y;
        }
    }
}
