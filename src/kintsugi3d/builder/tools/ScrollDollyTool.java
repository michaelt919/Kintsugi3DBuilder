/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.tools;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.window.Canvas3D;
import kintsugi3d.gl.window.listeners.ScrollListener;
import kintsugi3d.builder.state.ExtendedCameraModel;

public class ScrollDollyTool implements ScrollListener
{
    private static final float SENSITIVITY = 0.001f;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<ScrollDollyTool>
    {
        @Override
        public ScrollDollyTool create()
        {
            return new ScrollDollyTool(getCameraModel());
        }
    }

    static ToolBuilder<ScrollDollyTool> getBuilder()
    {
        return new ScrollDollyTool.Builder();
    }

    private ScrollDollyTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void scroll(Canvas3D<? extends Context<?>> canvas, double xOffset, double yOffset)
    {
        cameraModel.setLog10Distance((float) (cameraModel.getLog10Distance() - 0.5 * SENSITIVITY * yOffset));
    }
}
