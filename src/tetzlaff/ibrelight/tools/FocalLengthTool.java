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

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;

final class FocalLengthTool implements DragTool
{
    private static final double FOCAL_LENGTH_SENSITIVITY = 0.0625;
    private double focalLengthSensitivityAdjusted;

    private double oldLog10FocalLength;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<FocalLengthTool>
    {
        @Override
        public FocalLengthTool build()
        {
            return new FocalLengthTool(getCameraModel());
        }
    }

    static ToolBuilder<FocalLengthTool> getBuilder()
    {
        return new Builder();
    }

    private FocalLengthTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;
        oldLog10FocalLength = Math.log10(cameraModel.getFocalLength());
        focalLengthSensitivityAdjusted = FOCAL_LENGTH_SENSITIVITY / windowSize.width;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        cameraModel.setFocalLength((float)Math.pow(10,
            oldLog10FocalLength - (float) Math.toDegrees((cursorPosition.y - this.mouseStart.y) * 0.5 * focalLengthSensitivityAdjusted)));
    }
}
