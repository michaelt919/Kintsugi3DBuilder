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
import kintsugi3d.builder.state.EnvironmentModel;

final class RotateEnvironmentTool implements DragTool
{
    private static final double ROTATE_SENSITIVITY = Math.PI; //todo: get from gui somehow
    private double rotateSensitivityAdjusted = 1.0;

    private float oldEnvironmentRotation;

    private CursorPosition mouseStart;

    private final EnvironmentModel environmentModel;

    private static class Builder extends ToolBuilderBase<RotateEnvironmentTool>
    {
        @Override
        public RotateEnvironmentTool create()
        {
            return new RotateEnvironmentTool(getEnvironmentModel());
        }
    }

    static ToolBuilder<RotateEnvironmentTool> getBuilder()
    {
        return new Builder();
    }

    private RotateEnvironmentTool(EnvironmentModel environmentModel)
    {
        this.environmentModel = environmentModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        this.mouseStart = cursorPosition;

        oldEnvironmentRotation = environmentModel.getEnvironmentRotation();
        rotateSensitivityAdjusted = ROTATE_SENSITIVITY / Math.min(canvasSize.width, canvasSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        environmentModel.setEnvironmentRotation((float)(oldEnvironmentRotation + (cursorPosition.x - mouseStart.x) * rotateSensitivityAdjusted));
    }
}
