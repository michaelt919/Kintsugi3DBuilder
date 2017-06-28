package tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.mvc.controllers.CameraController;
import tetzlaff.mvc.controllers.LightController;
import tetzlaff.mvc.models.ReadonlyCameraModel;

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.mvc.models.ReadonlyLightModel;

public class LookToolController implements LightController, CameraController, CursorPositionListener, MouseButtonPressListener, ScrollListener
{
    private int inversion = 1;
    private int primaryButtonIndex;
    private int secondaryButtonIndex;
    private float sensitivity;

    private float startX = Float.NaN;
    private float startY = Float.NaN;
    private float mouseScale = Float.NaN;

    private Matrix4 oldOrbitMatrix;
    private float oldLogScale;

    private LightModelX lightModelX;
    private CameraModelX cameraModelX;

    private final GlobalController globalController;

    public static interface Builder
    {
        Builder setSensitivity(float sensitivity);
        Builder setPrimaryButtonIndex(int primaryButtonIndex);
        Builder setSecondaryButtonIndex(int secondaryButtonIndex);
        Builder setGlobalControler(GlobalController globalControler);
        Builder setLightModelX(LightModelX lightModelX);
        Builder setCameraModelX(CameraModelX cameraModelX);
        Builder setWindow(Window window);
        LookToolController create();
    }

    private static class BuilderImpl implements Builder
    {
        private float sensitivity = 1.0f;
        private int primaryButtonIndex = 0;
        private int secondaryButtonIndex = 1;
        private GlobalController globalController;
        LightModelX lightModelX;
        CameraModelX cameraModelX;
        Window window;

        public Builder setSensitivity(float sensitivity)
        {
            this.sensitivity = sensitivity;
            return this;
        }

        public Builder setPrimaryButtonIndex(int primaryButtonIndex)
        {
            this.primaryButtonIndex = primaryButtonIndex;
            return this;
        }

        public Builder setSecondaryButtonIndex(int secondaryButtonIndex)
        {
            this.secondaryButtonIndex = secondaryButtonIndex;
            return this;
        }

        public LookToolController create()
        {
            LookToolController out = new LookToolController(sensitivity, primaryButtonIndex, secondaryButtonIndex, globalController,
                    lightModelX, cameraModelX);
            out.addAsWindowListener(window);
            return out;
        }

        @Override
        public Builder setGlobalControler(GlobalController globalControler) {
            this.globalController = globalControler;
            return this;
        }

        @Override
        public Builder setLightModelX(LightModelX lightModelX) {
            this.lightModelX = lightModelX;
            return this;
        }

        @Override
        public Builder setCameraModelX(CameraModelX cameraModelX) {
            this.cameraModelX = cameraModelX;
            return this;
        }

        @Override
        public Builder setWindow(Window window) {
            this.window = window;
            return this;
        }
    }

    public static Builder getBuilder()
    {
        return new BuilderImpl();
    }

    //------------------------------------------------------------------------------------------------------------------

    private LookToolController( float sensitivity, int primaryButtonIndex, int secondaryButtonIndex,
                               GlobalController globalController, LightModelX lightModelX, CameraModelX cameraModelX)
    {
        this.primaryButtonIndex = primaryButtonIndex;
        this.secondaryButtonIndex = secondaryButtonIndex;
        this.sensitivity = sensitivity;
        this.globalController = globalController;
        this.lightModelX = lightModelX;
        this.cameraModelX = cameraModelX;
    }

    @Override
    public void addAsWindowListener(Window<?> window)
    {
        window.addCursorPositionListener(this);
        window.addMouseButtonPressListener(this);
        window.addScrollListener(this);
    }

    @Override
    public ReadonlyCameraModel getCameraModel()
    {
        return this.cameraModelX;
    }

    @Override
    public ReadonlyLightModel getLightModel() {
        return lightModelX;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        System.out.println("Mouse button index pressed: " + buttonIndex);
        if (enabled() && (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex))
        {
            CursorPosition pos = window.getCursorPosition();
            WindowSize size = window.getWindowSize();
            this.startX = (float)pos.x;
            this.startY = (float)pos.y;
            this.mouseScale = (float)Math.PI * this.sensitivity / Math.min(size.width, size.height);
            this.oldOrbitMatrix = cameraModelX.getOrbit();
            this.oldLogScale = (float) (Math.log(cameraModelX.getZoom())/Math.log(2));
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos)
    {
        if (enabled())
        {
            if (this.primaryButtonIndex >= 0 && window.getMouseButtonState(primaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale) && (xpos != this.startX || ypos != this.startY))
                {
                    Vector3 rotationVector =
                            new Vector3(
                                    (float)(ypos - this.startY),
                                    (float)(xpos - this.startX),
                                    0.0f
                            );

                    this.cameraModelX.setOrbit(
                            Matrix4.rotateAxis(
                                    rotationVector.normalized(),
                                    this.mouseScale * rotationVector.length() * this.inversion
                            )
                                    .times(this.oldOrbitMatrix));
                }
            }
            else if (this.secondaryButtonIndex >= 0 && window.getMouseButtonState(secondaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale))
                {
                    this.cameraModelX.setOrbit(
                            Matrix4.rotateZ(this.mouseScale * (xpos - this.startX) * this.inversion)
                                    .times(this.oldOrbitMatrix));
                    double newLogScale = this.oldLogScale + this.mouseScale * (float)(ypos - this.startY);
                    this.cameraModelX.setZoom((float)(Math.pow(2, newLogScale)));
                }
            }
        }
    }

    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset)
    {
        if (enabled())
        {
            cameraModelX.setZoom(cameraModelX.getZoom() * (float) (Math.pow(2, (sensitivity * (yoffset) / 256.0 ))));
        }
    }

    public void setInverted(boolean inverted)
    {
        this.inversion = inverted ? -1 : 1;
    }

    private Boolean enabled(){
        return globalController.getTool() == Tool.LOOK;
    }
}
