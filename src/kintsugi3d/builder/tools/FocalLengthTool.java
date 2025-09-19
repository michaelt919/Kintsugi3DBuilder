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

package kintsugi3d.builder.tools;

import kintsugi3d.builder.state.ManipulableViewpointModel;
import kintsugi3d.gl.window.CanvasSize;
import kintsugi3d.gl.window.CursorPosition;

final class FocalLengthTool implements DragTool
{
    private static final double FOCAL_LENGTH_SENSITIVITY = 0.0625;
    private double focalLengthSensitivityAdjusted;

    private double oldLog10FocalLength;

    private CursorPosition mouseStart;

    private final ManipulableViewpointModel cameraModel;

    private static class Builder extends ToolBuilderBase<FocalLengthTool>
    {
        @Override
        public FocalLengthTool create()
        {
            return new FocalLengthTool(getCameraModel());
        }
    }

    static ToolBuilder<FocalLengthTool> getBuilder()
    {
        return new Builder();
    }

    private FocalLengthTool(ManipulableViewpointModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;
        oldLog10FocalLength = Math.log10(cameraModel.getFocalLength());
        focalLengthSensitivityAdjusted = FOCAL_LENGTH_SENSITIVITY / canvasSize.width;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        cameraModel.setFocalLength((float)Math.pow(10,
            oldLog10FocalLength - (float) Math.toDegrees((cursorPosition.y - this.mouseStart.y) * 0.5 * focalLengthSensitivityAdjusted)));
    }
}
