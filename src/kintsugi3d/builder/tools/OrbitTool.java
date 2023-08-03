/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.tools;//Created by alexk on 7/24/2017.

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.window.CanvasSize;
import kintsugi3d.gl.window.CursorPosition;
import kintsugi3d.builder.state.ExtendedCameraModel;

final class OrbitTool implements DragTool
{
    private static final double ORBIT_SENSITIVITY = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    private float oldTwist;
    private Matrix4 oldOrbitMatrix;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<OrbitTool>
    {
        @Override
        public OrbitTool create()
        {
            return new OrbitTool(getCameraModel());
        }
    }

    static ToolBuilder<OrbitTool> getBuilder()
    {
        return new Builder();
    }

    private OrbitTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;

        oldOrbitMatrix = cameraModel.getOrbit();
        oldTwist = cameraModel.getTwist();
        orbitSensitivityAdjusted = ORBIT_SENSITIVITY / Math.min(canvasSize.width, canvasSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        if (cursorPosition.y != mouseStart.y || cursorPosition.x != mouseStart.x)
        {
            Vector3 rotationVector = new Vector3(
                (float) (cursorPosition.y - mouseStart.y),
                (float) (cursorPosition.x - mouseStart.x),
                0.0f);

            cameraModel.setOrbit(
                Matrix4.rotateAxis(rotationVector.normalized(), rotationVector.length() * orbitSensitivityAdjusted)
                    .times(oldOrbitMatrix));

            cameraModel.setTwist(oldTwist);
        }
    }
}
