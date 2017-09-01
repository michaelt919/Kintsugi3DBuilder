package tetzlaff.mvc.old.controllers.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.*;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.mvc.old.controllers.CameraController;
import tetzlaff.mvc.old.models.TrackballModel;

public final class TrackballController implements CameraController, CursorPositionListener, MouseButtonPressListener, ScrollListener
{
    private int inversion = 1;
    private boolean enabled = true;
    private final int primaryButtonIndex;
    private final int secondaryButtonIndex;
    private final float sensitivity;

    private float startX = Float.NaN;
    private float startY = Float.NaN;
    private float mouseScale = Float.NaN;

    private Matrix4 oldTrackballMatrix;
    private float oldLogScale;

    private final TrackballModel model;

    public interface Builder
    {
        Builder setSensitivity(float sensitivity);
        Builder setPrimaryButtonIndex(int primaryButtonIndex);
        Builder setSecondaryButtonIndex(int secondaryButtonIndex);
        Builder setModel(TrackballModel model);
        TrackballController create();
    }

    private static class BuilderImpl implements Builder
    {
        private float sensitivity = 1.0f;
        private int primaryButtonIndex = 0;
        private int secondaryButtonIndex = 1;
        private TrackballModel model;

        @Override
        public Builder setSensitivity(float sensitivity)
        {
            this.sensitivity = sensitivity;
            return this;
        }

        @Override
        public Builder setPrimaryButtonIndex(int primaryButtonIndex)
        {
            this.primaryButtonIndex = primaryButtonIndex;
            return this;
        }

        @Override
        public Builder setSecondaryButtonIndex(int secondaryButtonIndex)
        {
            this.secondaryButtonIndex = secondaryButtonIndex;
            return this;
        }

        @Override
        public Builder setModel(TrackballModel model)
        {
            this.model = model;
            return this;
        }

        @Override
        public TrackballController create()
        {
            if (this.model == null)
            {
                this.model = new TrackballModel();
            }

            return new TrackballController(model, sensitivity, primaryButtonIndex, secondaryButtonIndex);
        }
    }

    public static Builder getBuilder()
    {
        return new BuilderImpl();
    }

    private TrackballController(TrackballModel model, float sensitivity, int primaryButtonIndex, int secondaryButtonIndex)
    {
        this.primaryButtonIndex = primaryButtonIndex;
        this.secondaryButtonIndex = secondaryButtonIndex;
        this.sensitivity = sensitivity;
        this.model = model;
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
        return this.model;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (enabled && (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex))
        {
            CursorPosition pos = window.getCursorPosition();
            WindowSize size = window.getWindowSize();
            this.startX = (float)pos.x;
            this.startY = (float)pos.y;
            this.mouseScale = (float)Math.PI * this.sensitivity / Math.min(size.width, size.height);
            this.oldTrackballMatrix = model.getTrackballMatrix();
            this.oldLogScale = model.getLogScale();
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (enabled)
        {
            if (this.primaryButtonIndex >= 0 && window.getMouseButtonState(primaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale) && (xPos != this.startX || yPos != this.startY))
                {
                    Vector3 rotationVector =
                        new Vector3(
                            (float)(yPos - this.startY),
                            (float)(xPos - this.startX),
                            0.0f
                        );

                    this.model.setTrackballMatrix(
                        Matrix4.rotateAxis(
                            rotationVector.normalized(),
                            this.mouseScale * rotationVector.length() * this.inversion
                        )
                        .times(this.oldTrackballMatrix));
                }
            }
            else if (this.secondaryButtonIndex >= 0 && window.getMouseButtonState(secondaryButtonIndex) == MouseButtonState.Pressed)
            {
                if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale))
                {
                    this.model.setTrackballMatrix(
                        Matrix4.rotateZ(this.mouseScale * (xPos - this.startX) * this.inversion)
                            .times(this.oldTrackballMatrix));

                    this.model.setLogScale(this.oldLogScale + this.mouseScale * (float)(yPos - this.startY));
                }
            }
        }
    }

    @Override
    public void scroll(Window<?> window, double xOffset, double yOffset)
    {
        if (enabled)
        {
            model.setLogScale(model.getLogScale() + sensitivity / 256.0f * (float) yOffset);
        }
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setInverted(boolean inverted)
    {
        this.inversion = inverted ? -1 : 1;
    }
}
