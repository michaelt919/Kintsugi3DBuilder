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

final class TwistTool implements DragTool
{
    private static final double TWIST_SENSITIVITY = Math.PI;
    private double twistSensitivityAdjusted;

    private float oldTwist;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<TwistTool>
    {
        @Override
        public TwistTool build()
        {
            return new TwistTool(getCameraModel());
        }
    }

    static ToolBuilder<TwistTool> getBuilder()
    {
        return new Builder();
    }

    private TwistTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;
        oldTwist = cameraModel.getTwist();
        twistSensitivityAdjusted = TWIST_SENSITIVITY / windowSize.width;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        cameraModel.setTwist(oldTwist + (float) Math.toDegrees((cursorPosition.x - this.mouseStart.x) * twistSensitivityAdjusted));
    }
}
