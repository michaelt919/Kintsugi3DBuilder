/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.tools;//Created by alexk on 7/24/2017.

import kintsugi3d.builder.state.ExtendedCameraModel;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.window.CanvasSize;
import kintsugi3d.gl.window.CursorPosition;

/*
not this work as intended at a zoom of 0.5
 */
final class PanTool implements DragTool
{
    private static final float PAN_SENSITIVITY = 1.0f;
    private float panSensitivityAdjusted = 1.0f;

    private Vector3 oldCenter = Vector3.ZERO;
    private Matrix4 orbit;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;
    private final SettingsModel settingsModel;

    private static class Builder extends ToolBuilderBase<PanTool>
    {
        @Override
        public PanTool create()
        {
            return new PanTool(getCameraModel(), getSettingsModel());
        }
    }

    static ToolBuilder<PanTool> getBuilder()
    {
        return new Builder();
    }

    private PanTool(ExtendedCameraModel cameraModel, SettingsModel settingsModel)
    {
        this.cameraModel = cameraModel;
        this.settingsModel = settingsModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;

        oldCenter = cameraModel.getTarget();
        orbit = cameraModel.getOrbit();

        panSensitivityAdjusted = PAN_SENSITIVITY / Math.min(canvasSize.width, canvasSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        Vector2 moveVector = new Vector2(
            (float) (cursorPosition.x - mouseStart.x) * panSensitivityAdjusted,
            (float) (mouseStart.y - cursorPosition.y) * panSensitivityAdjusted);

        Vector3 worldMoveVector = orbit.transpose().times(moveVector.asVector4(0.0f,0.0f)).getXYZ();
        cameraModel.setTarget(oldCenter.minus(worldMoveVector));
    }
}
