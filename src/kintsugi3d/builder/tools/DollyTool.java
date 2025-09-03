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

final class DollyTool implements DragTool
{
    private static final double DOLLY_SENSITIVITY = Math.PI;
    private double dollySensitivityAdjusted;

    private float oldLog10Distance;

    private CursorPosition mouseStart;

    private final ManipulableViewpointModel cameraModel;

    private static class Builder extends ToolBuilderBase<DollyTool>
    {
        @Override
        public DollyTool create()
        {
            return new DollyTool(getCameraModel());
        }
    }

    static ToolBuilder<DollyTool> getBuilder()
    {
        return new Builder();
    }

    private DollyTool(ManipulableViewpointModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;
        oldLog10Distance = cameraModel.getLog10Distance();
        dollySensitivityAdjusted = DOLLY_SENSITIVITY / canvasSize.height;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        cameraModel.setLog10Distance((float) (oldLog10Distance - 0.5 * dollySensitivityAdjusted * (this.mouseStart.y - cursorPosition.y)));
    }
}
