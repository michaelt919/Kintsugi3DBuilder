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

import kintsugi3d.builder.state.ExtendedObjectModel;
import kintsugi3d.builder.state.ExtendedViewpointModel;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.window.CanvasSize;
import kintsugi3d.gl.window.CursorPosition;

final class ObjectRotationTool implements DragTool
{
    private static final double ORBIT_SENSITIVITY = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    private Matrix4 oldOrbitMatrix;
    private Matrix4 cameraOrbitMatrix;

    private CursorPosition mouseStart;

    private final ExtendedViewpointModel cameraModel;
    private final ExtendedObjectModel objectModel;

    private static class Builder extends ToolBuilderBase<ObjectRotationTool>
    {
        @Override
        public ObjectRotationTool create()
        {
            return new ObjectRotationTool(getCameraModel(), getObjectModel());
        }
    }

    static ToolBuilder<ObjectRotationTool> getBuilder()
    {
        return new Builder();
    }

    private ObjectRotationTool(ExtendedViewpointModel cameraModel, ExtendedObjectModel objectModel)
    {
        this.cameraModel = cameraModel;
        this.objectModel = objectModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;

        oldOrbitMatrix = objectModel.getOrbit();
        cameraOrbitMatrix = cameraModel.getOrbit();
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

            objectModel.setOrbit(
                cameraOrbitMatrix.transpose()
                    .times(Matrix4.rotateAxis(rotationVector.normalized(), rotationVector.length() * orbitSensitivityAdjusted))
                    .times(cameraOrbitMatrix)
                    .times(oldOrbitMatrix));
        }
    }
}
