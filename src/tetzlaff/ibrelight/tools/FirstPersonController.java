/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.tools;

import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.Canvas3D;
import tetzlaff.gl.window.CanvasSize;
import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.KeyReleaseListener;
import tetzlaff.models.CameraModel;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.impl.SimpleCameraModel;

public class FirstPersonController implements KeyPressListener, KeyReleaseListener, CursorPositionListener
{
    private boolean enabled;

    private final CameraModel model;

    private Vector3 velocity;
    private Vector3 position;

    private double lastCursorX;
    private double lastCursorY;

    private float theta;
    private float phi;

    private boolean ignoreSensitivity = false;
    private final float sensitivity = 0.5f;
    private final float speed = 0.01f;

    public FirstPersonController()
    {
        this(new SimpleCameraModel());
    }

    public FirstPersonController(CameraModel cameraModel)
    {
        this.model = cameraModel;

        this.velocity = new Vector3(0.0f, 0.0f, 0.0f);
        this.position = new Vector3(0.0f, 0.0f, 0.0f);

        this.lastCursorX = Float.NaN;
        this.lastCursorY = Float.NaN;

        this.theta = 0.0f;
        this.phi = 0.0f;
    }

    public void addAsWindowListener(Canvas3D<? extends tetzlaff.gl.core.Context<?>> canvas)
    {
        canvas.addKeyPressListener(this);
        canvas.addKeyReleaseListener(this);
        canvas.addCursorPositionListener(this);
    }

    public boolean getEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        if (!enabled)
        {
            this.lastCursorX = Float.NaN;
            this.lastCursorY = Float.NaN;
            this.velocity = new Vector3(0.0f, 0.0f, 0.0f);
        }
    }

    public ReadonlyCameraModel getModel()
    {
        return this.model;
    }

    @Override
    public void keyPressed(Canvas3D<? extends tetzlaff.gl.core.Context<?>> canvas, Key key, ModifierKeys mods)
    {
        if (enabled)
        {
            if (key == Key.W)
            {
                velocity = velocity.plus(new Vector3(0.0f, 0.0f, -1.0f));
            }

            if (key == Key.S)
            {
                velocity = velocity.plus(new Vector3(0.0f, 0.0f, 1.0f));
            }

            if (key == Key.D)
            {
                velocity = velocity.plus(new Vector3(1.0f, 0.0f, 0.0f));
            }

            if (key == Key.A)
            {
                velocity = velocity.plus(new Vector3(-1.0f, 0.0f, 0.0f));
            }

            if (key == Key.E)
            {
                velocity = velocity.plus(new Vector3(0.0f, 1.0f, 0.0f));
            }

            if (key == Key.Q)
            {
                velocity = velocity.plus(new Vector3(0.0f, -1.0f, 0.0f));
            }
        }

        // Reset regardless of if enabled
        if (key == Key.X)
        {
            theta = 0.0f;
            phi = 0.0f;
            position = new Vector3(0.0f, 0.0f, 0.0f);
            this.model.setLookMatrix(Matrix4.IDENTITY);
        }

        ignoreSensitivity = mods.getControlModifier();
    }

    @Override
    public void keyReleased(Canvas3D<? extends tetzlaff.gl.core.Context<?>> canvas, Key key, ModifierKeys mods)
    {
        if (enabled)
        {
            if (key == Key.W)
            {
                velocity = velocity.minus(new Vector3(0.0f, 0.0f, -1.0f));
            }

            if (key == Key.S)
            {
                velocity = velocity.minus(new Vector3(0.0f, 0.0f, 1.0f));
            }

            if (key == Key.D)
            {
                velocity = velocity.minus(new Vector3(1.0f, 0.0f, 0.0f));
            }

            if (key == Key.A)
            {
                velocity = velocity.minus(new Vector3(-1.0f, 0.0f, 0.0f));
            }

            if (key == Key.E)
            {
                velocity = velocity.minus(new Vector3(0.0f, 1.0f, 0.0f));
            }

            if (key == Key.Q)
            {
                velocity = velocity.minus(new Vector3(0.0f, -1.0f, 0.0f));
            }
        }

        ignoreSensitivity = mods.getControlModifier();
    }

    @Override
    public void cursorMoved(Canvas3D<? extends tetzlaff.gl.core.Context<?>> canvas, double xPos, double yPos)
    {
        if (enabled)
        {
            CanvasSize size = canvas.getSize();

            if (!Double.isNaN(lastCursorX) && !Double.isNaN(lastCursorY))
            {
                theta += (ignoreSensitivity ? 1.0f : sensitivity) * 2 * Math.PI * (xPos - lastCursorX) / size.width;
                phi += (ignoreSensitivity ? 1.0f : sensitivity) * Math.PI * (yPos - lastCursorY) / size.height;

                if (theta < 0.0f)
                {
                    theta += 2 * Math.PI;
                }

                if (theta > 2 * Math.PI)
                {
                    theta -= 2 * Math.PI;
                }

                if (phi > Math.PI / 2)
                {
                    phi = (float)Math.PI / 2;
                }

                if (phi < -Math.PI / 2)
                {
                    phi = -(float)Math.PI / 2;
                }
            }

            lastCursorX = xPos;
            lastCursorY = yPos;
        }
    }

    public void update()
    {
        if (enabled)
        {
            Matrix3 rotation = Matrix3.rotateX(phi).times(Matrix3.rotateY(theta));

            if (velocity.dot(velocity) > 0.0f)
            {
                position = position.plus(rotation.transpose().times(velocity.normalized().times(speed)));
            }

            this.model.setLookMatrix(rotation.asMatrix4().times(Matrix4.translate(position.negated())));
        }
    }
}
