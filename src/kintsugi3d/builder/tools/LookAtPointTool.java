/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.tools;//Created by alexk on 8/8/2017.

import kintsugi3d.gl.window.CanvasSize;
import kintsugi3d.gl.window.CursorPosition;
import kintsugi3d.builder.state.ExtendedCameraModel;
import kintsugi3d.builder.state.SceneViewport;
import kintsugi3d.builder.state.SceneViewportModel;

final class LookAtPointTool implements DragTool
{
    private final ExtendedCameraModel cameraModel;
    private final SceneViewportModel sceneViewportModel;

    private static class Builder extends ToolBuilderBase<LookAtPointTool>
    {
        @Override
        public LookAtPointTool create()
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
    public void mouseButtonPressed(CursorPosition cursorPosition, CanvasSize canvasSize)
    {
        double normalizedX = cursorPosition.x / canvasSize.width;
        double normalizedY = cursorPosition.y / canvasSize.height;

        SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();
        Object clickedObject = sceneViewport.getObjectAtCoordinates(normalizedX, normalizedY);
        if (clickedObject instanceof String && "IBRObject".equals(clickedObject))
        {
            cameraModel.setTarget(sceneViewport.get3DPositionAtCoordinates(normalizedX, normalizedY));
        }
    }
}
