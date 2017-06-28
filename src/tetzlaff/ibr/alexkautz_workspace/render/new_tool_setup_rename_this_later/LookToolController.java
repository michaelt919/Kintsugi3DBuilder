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
    private final int primaryButtonIndex;
    private final int secondaryButtonIndex;
    private final int tertiaryButtonIndex;
    private float sensitivityScrollWheel;
    private float sensitivityOrbit;

    private float startX = Float.NaN;
    private float startY = Float.NaN;
    private float mouseScrollScale = Float.NaN;
    private float mouseOrbitScale = Float.NaN;

    private Matrix4 oldOrbitMatrix;
    private float oldLogScale;
    private Vector3 oldOffSet;

    private final LightModelX lightModelX;
    private final CameraModelX cameraModelX;

    private final GlobalController globalController;

    public static interface Builder
    {
        Builder setSensitivityScrollWheel(float sensitivityScrollWheel);
        Builder setSensitivityOrbit(float sensitivityOrbit);
        Builder setPrimaryButtonIndex(int primaryButtonIndex);
        Builder setSecondaryButtonIndex(int secondaryButtonIndex);
        Builder setTertiaryButtonIndex(int tertiaryButtonIndex);
        Builder setGlobalControler(GlobalController globalControler);
        Builder setLightModelX(LightModelX lightModelX);
        Builder setCameraModelX(CameraModelX cameraModelX);
        Builder setWindow(Window window);
        LookToolController create();
    }

    private static class BuilderImpl implements Builder
    {
        private float sensitivityScrollWheel = 1.0f;
        private float sensitivityOrbit = 1.0f;
        private int primaryButtonIndex = 0;
        private int secondaryButtonIndex = 1;
        private int tertiaryButtonIndex = 2;
        private GlobalController globalController;
        private LightModelX lightModelX;
        private CameraModelX cameraModelX;
        private Window window;

        public Builder setSensitivityScrollWheel(float sensitivityScrollWheel)
        {
            this.sensitivityScrollWheel = sensitivityScrollWheel;
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
            LookToolController out = new LookToolController(sensitivityScrollWheel, sensitivityOrbit, primaryButtonIndex, secondaryButtonIndex, tertiaryButtonIndex, globalController,
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

        @Override
        public Builder setTertiaryButtonIndex(int tertiaryButtonIndex) {
            this.tertiaryButtonIndex = tertiaryButtonIndex;
            return this;
        }

        @Override
        public Builder setSensitivityOrbit(float sensitivityOrbit) {
            this.sensitivityOrbit = sensitivityOrbit;
            return this;
        }
    }

    public static Builder getBuilder()
    {
        return new BuilderImpl();
    }

    //------------------------------------------------------------------------------------------------------------------

    private LookToolController(float sensitivityScrollWheel, float sensitivityOrbit, int primaryButtonIndex, int secondaryButtonIndex, int tertiaryButtonIndex,
                               GlobalController globalController, LightModelX lightModelX, CameraModelX cameraModelX)
    {
        this.primaryButtonIndex = primaryButtonIndex;
        this.secondaryButtonIndex = secondaryButtonIndex;
        this.tertiaryButtonIndex = tertiaryButtonIndex;
        this.sensitivityScrollWheel = sensitivityScrollWheel;
        this.sensitivityOrbit = sensitivityOrbit;
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
        if (enabled() && (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex || buttonIndex == this.tertiaryButtonIndex))
        {
            CursorPosition pos = window.getCursorPosition();
            WindowSize size = window.getWindowSize();
            this.startX = (float)pos.x;
            this.startY = (float)pos.y;
            this.mouseScrollScale = (float)Math.PI * this.sensitivityScrollWheel / Math.min(size.width, size.height);
            this.mouseOrbitScale = (float)Math.PI * this.sensitivityOrbit / Math.min(size.width, size.height);
            this.oldOrbitMatrix = cameraModelX.getOrbit();
            this.oldLogScale = (float) (Math.log(cameraModelX.getZoom())/Math.log(2));
            this.oldOffSet = cameraModelX.getOffSet();
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos)
    {
        if (enabled())
        {
            if (this.primaryButtonIndex >= 0 && window.getMouseButtonState(primaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseOrbitScale) && (xpos != this.startX || ypos != this.startY))
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
                                    this.mouseOrbitScale * rotationVector.length() * this.inversion
                            )
                                    .times(this.oldOrbitMatrix));
                }
            }
            else if (this.secondaryButtonIndex >= 0 && window.getMouseButtonState(secondaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseOrbitScale))
                {
                    this.cameraModelX.setOrbit(
                            Matrix4.rotateZ(this.mouseOrbitScale * (xpos - this.startX) * this.inversion)
                                    .times(this.oldOrbitMatrix));
                    double newLogScale = this.oldLogScale + this.mouseOrbitScale * (float)(ypos - this.startY);
                    this.cameraModelX.setZoom((float)(Math.pow(2, newLogScale)));
                }
            }
            else if (this.tertiaryButtonIndex >= 0 && window.getMouseButtonState(tertiaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && (xpos != this.startX || ypos != this.startY)) {
                    //System.out.println("Panning Time");
                    final float scale = 0.6f/(cameraModelX.getZoom()); //TODO make this panning exact.
                    //TODO check for panning distortion
                    Vector3 addedTranslation = new Vector3(
                            (((float)(xpos - this.startX))/((float)(window.getWindowSize().height))),
                            (((float)(this.startY - ypos))/((float)(window.getWindowSize().height))),
                            0.0f
                    );
                    cameraModelX.setOffSet(oldOffSet.plus(addedTranslation.times(scale)));

                    //System.out.println("Start y: " + this.startY + " Mouse Scale: " + mouseScrollScale + " change: " + addedTranslation.times(this.mouseScrollScale).y + " final " + cameraModelX.getOffSet().y);

                    //System.out.println("Zoom: " + cameraModelX.getZoom());
                }


            }
        }
    }



    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset)
    {
        if (enabled())
        {
            cameraModelX.setZoom(cameraModelX.getZoom() * (float) (Math.pow(2, (sensitivityScrollWheel * (yoffset) / 256.0 ))));
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
