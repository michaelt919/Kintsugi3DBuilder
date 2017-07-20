package tetzlaff.ibr.rendering2.tools;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.ibr.rendering2.CameraModel2;
import tetzlaff.ibr.rendering2.LightModel2;
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

    private final LightModel2 lightModel2;
    private final CameraModel2 cameraModel2;

    private final ToolModel2 toolModel2;

    public static interface Builder
    {
        Builder setSensitivityScrollWheel(float sensitivityScrollWheel);
        Builder setSensitivityOrbit(float sensitivityOrbit);
        Builder setPrimaryButtonIndex(int primaryButtonIndex);
        Builder setSecondaryButtonIndex(int secondaryButtonIndex);
        Builder setTertiaryButtonIndex(int tertiaryButtonIndex);
        Builder setGlobalController(ToolModel2 toolModel2);
        Builder setLightModelX(LightModel2 lightModel2);
        Builder setCameraModelX(CameraModel2 cameraModel2);
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
        private ToolModel2 toolModel2;
        private LightModel2 lightModel2;
        private CameraModel2 cameraModel2;
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
            LookToolController out = new LookToolController(sensitivityScrollWheel, sensitivityOrbit, primaryButtonIndex, secondaryButtonIndex, tertiaryButtonIndex, toolModel2,
                    lightModel2, cameraModel2);
            out.addAsWindowListener(window);
            return out;
        }

        @Override
        public Builder setGlobalController(ToolModel2 toolModel2) {
            this.toolModel2 = toolModel2;
            return this;
        }

        @Override
        public Builder setLightModelX(LightModel2 lightModel2) {
            this.lightModel2 = lightModel2;
            return this;
        }

        @Override
        public Builder setCameraModelX(CameraModel2 cameraModel2) {
            this.cameraModel2 = cameraModel2;
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
                               ToolModel2 toolModel2, LightModel2 lightModel2, CameraModel2 cameraModel2)
    {
        this.primaryButtonIndex = primaryButtonIndex;
        this.secondaryButtonIndex = secondaryButtonIndex;
        this.tertiaryButtonIndex = tertiaryButtonIndex;
        this.sensitivityScrollWheel = sensitivityScrollWheel;
        this.sensitivityOrbit = sensitivityOrbit;
        this.toolModel2 = toolModel2;
        this.lightModel2 = lightModel2;
        this.cameraModel2 = cameraModel2;
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
        return this.cameraModel2;
    }

    @Override
    public ReadonlyLightModel getLightModel() {
        return lightModel2;
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
            this.oldOrbitMatrix = cameraModel2.getOrbitPlus();
            this.oldLogScale = (float) (Math.log(cameraModel2.getZoom())/Math.log(2));
            this.oldOffSet = cameraModel2.getOffSet();
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

                    this.cameraModel2.setOrbitPlus(
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
                    this.cameraModel2.setOrbitPlus(
                            Matrix4.rotateZ(this.mouseOrbitScale * (xpos - this.startX) * this.inversion)
                                    .times(this.oldOrbitMatrix));
                    double newLogScale = this.oldLogScale + this.mouseOrbitScale * (float)(ypos - this.startY);
                    this.cameraModel2.setZoom((float)(Math.pow(2, newLogScale)));
                }
            }
            else if (this.tertiaryButtonIndex >= 0 && window.getMouseButtonState(tertiaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && (xpos != this.startX || ypos != this.startY)) {
                    //System.out.println("Panning Time");
                    final float scale = 0.6f/(cameraModel2.getZoom()); //TODO make this panning exact.
                    //TODO check for panning distortion
                    Vector3 addedTranslation = new Vector3(
                            (((float)(xpos - this.startX))/((float)(window.getWindowSize().height))),
                            (((float)(this.startY - ypos))/((float)(window.getWindowSize().height))),
                            0.0f
                    );
                    cameraModel2.setOffSet(oldOffSet.plus(addedTranslation.times(scale)));

                    //System.out.println("Start y: " + this.startY + " Mouse Scale: " + mouseScrollScale + " change: " + addedTranslation.times(this.mouseScrollScale).y + " final " + cameraModel2.getOffSet().y);

                    //System.out.println("Zoom: " + cameraModel2.getZoom());
                }


            }
        }
    }



    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset)
    {
        if (enabled())
        {
            cameraModel2.setZoom(cameraModel2.getZoom() * (float) (Math.pow(2, (sensitivityScrollWheel * (yoffset) / 256.0 ))));
        }
    }

    public void setInverted(boolean inverted)
    {
        this.inversion = inverted ? -1 : 1;
    }

    private Boolean enabled(){
        return toolModel2.getTool() == Tool.LOOK;
    }
}
