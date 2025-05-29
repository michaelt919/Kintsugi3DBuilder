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

import kintsugi3d.gl.window.CanvasSize;
import kintsugi3d.gl.window.CursorPosition;
import kintsugi3d.builder.state.ExtendedCameraModel;

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
        public TwistTool create()
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
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;
        oldTwist = cameraModel.getTwist();
        twistSensitivityAdjusted = TWIST_SENSITIVITY / canvasSize.width;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        cameraModel.setTwist(oldTwist + (float) Math.toDegrees((cursorPosition.x - this.mouseStart.x) * twistSensitivityAdjusted));
    }
}
