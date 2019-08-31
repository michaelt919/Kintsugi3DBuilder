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

package tetzlaff.ibrelight.tools;//Created by alexk on 8/8/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.SceneViewport;
import tetzlaff.models.SceneViewportModel;

final class LookAtPointTool implements DragTool
{
    private final ExtendedCameraModel cameraModel;
    private final SceneViewportModel sceneViewportModel;

    private static class Builder extends ToolBuilderBase<LookAtPointTool>
    {
        @Override
        public LookAtPointTool build()
        {
            return new LookAtPointTool(getCameraModel(), getSceneViewportModel());
        }
    }

    static ToolBuilder<LookAtPointTool> getBuilder()
    {
        return new Builder();
    }

    private LookAtPointTool(ExtendedCameraModel cameraModel, SceneViewportModel sceneViewportModel)
    {
        this.cameraModel = cameraModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        double normalizedX = cursorPosition.x / windowSize.width;
        double normalizedY = cursorPosition.y / windowSize.height;

        SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();
        Object clickedObject = sceneViewport.getObjectAtCoordinates(normalizedX, normalizedY);
        if (clickedObject instanceof String && "IBRObject".equals(clickedObject))
        {
            cameraModel.setTarget(sceneViewport.get3DPositionAtCoordinates(normalizedX, normalizedY));
        }
    }
}
