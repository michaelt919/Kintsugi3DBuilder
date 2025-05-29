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

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.window.CanvasSize;
import kintsugi3d.gl.window.CursorPosition;
import kintsugi3d.builder.state.ExtendedCameraModel;
import kintsugi3d.builder.state.ExtendedObjectModel;

final class ObjectCenterTool implements DragTool
{
    private static final double PAN_SENSITIVITY = 1.0;
    private double panSensitivityAdjusted = 1.0;

    private Vector3 oldCenter = Vector3.ZERO;
    private Matrix4 orbit;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;
    private final ExtendedObjectModel objectModel;

    private static class Builder extends ToolBuilderBase<ObjectCenterTool>
    {
        @Override
        public ObjectCenterTool create()
        {
            return new ObjectCenterTool(getCameraModel(), getObjectModel());
        }
    }

    static ToolBuilder<ObjectCenterTool> getBuilder()
    {
        return new Builder();
    }

    private ObjectCenterTool(ExtendedCameraModel cameraModel, ExtendedObjectModel objectModel)
    {
        this.cameraModel = cameraModel;
        this.objectModel = objectModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;

        oldCenter = objectModel.getCenter();
        orbit = cameraModel.getOrbit().times(objectModel.getOrbit());

        panSensitivityAdjusted = PAN_SENSITIVITY / Math.min(canvasSize.width, canvasSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        Vector3 moveVector = new Vector3(
                (float) (cursorPosition.x - mouseStart.x),
                (float) (mouseStart.y - cursorPosition.y),
                0.0f);

        moveVector = moveVector.times((float) panSensitivityAdjusted);
        Vector3 worldMoveVector = orbit.transpose().times(moveVector.asDirection()).getXYZ();
        objectModel.setCenter(oldCenter.minus(worldMoveVector));
    }
}
