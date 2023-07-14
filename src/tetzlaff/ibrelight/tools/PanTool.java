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

package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.CanvasSize;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.SettingsModel;

/*
not this work as intended at a zoom of 0.5
 */
final class PanTool implements DragTool
{
    private static final float PAN_SENSITIVITY = 1.0f;
    private float panSensitivityAdjusted = 1.0f;

    private Vector2 oldLightCenter = Vector2.ZERO;
    private Vector3 oldCenter = Vector3.ZERO;
    private Matrix4 orbit;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;
    private final SettingsModel settingsModel;

    private static class Builder extends ToolBuilderBase<PanTool>
    {
        @Override
        public PanTool build()
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
        oldLightCenter = settingsModel.get("currentLightCalibration", Vector2.class);

        panSensitivityAdjusted = PAN_SENSITIVITY / Math.min(canvasSize.width, canvasSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        Vector2 moveVector = new Vector2(
            (float) (cursorPosition.x - mouseStart.x) * panSensitivityAdjusted,
            (float) (mouseStart.y - cursorPosition.y) * panSensitivityAdjusted);

        if (settingsModel.getBoolean("lightCalibrationMode"))
        {
            settingsModel.set("currentLightCalibration", oldLightCenter.minus(moveVector));
        }
        else
        {
            Vector3 worldMoveVector = orbit.transpose().times(moveVector.asVector4(0.0f,0.0f)).getXYZ();
            cameraModel.setTarget(oldCenter.minus(worldMoveVector));
        }
    }
}
