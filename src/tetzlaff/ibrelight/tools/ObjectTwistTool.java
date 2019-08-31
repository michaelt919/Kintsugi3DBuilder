/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedObjectModel;

final class ObjectTwistTool implements DragTool
{
    private static final double ORBIT_SENSITIVITY = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    private Matrix4 oldOrbitMatrix;
    private Matrix4 cameraOrbitMatrix;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;
    private final ExtendedObjectModel objectModel;

    private static class Builder extends ToolBuilderBase<ObjectTwistTool>
    {
        @Override
        public ObjectTwistTool build()
        {
            return new ObjectTwistTool(getCameraModel(), getObjectModel());
        }
    }

    static ToolBuilder<ObjectTwistTool> getBuilder()
    {
        return new Builder();
    }

    private ObjectTwistTool(ExtendedCameraModel cameraModel, ExtendedObjectModel objectModel)
    {
        this.cameraModel = cameraModel;
        this.objectModel = objectModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;

        oldOrbitMatrix = objectModel.getOrbit();
        cameraOrbitMatrix = cameraModel.getOrbit();
        orbitSensitivityAdjusted = ORBIT_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        if (cursorPosition.x != mouseStart.x)
        {
            objectModel.setOrbit(
                cameraOrbitMatrix.transpose()
                    .times(Matrix4.rotateZ((cursorPosition.x - mouseStart.x) * orbitSensitivityAdjusted))
                    .times(cameraOrbitMatrix)
                    .times(oldOrbitMatrix));
        }
    }
}
