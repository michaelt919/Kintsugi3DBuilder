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

package kintsugi3d.builder.export.general;

import kintsugi3d.gl.core.Context;

import java.io.File;

public interface RequestFactory
{
    RenderRequestBuilder buildSingleFrameRenderRequest(File fragmentShader, File outputDirectory, String outputImageName);

    RenderRequestBuilder buildMultiframeRenderRequest(File fragmentShader, File outputDirectory, int frameCount);

    RenderRequestBuilder buildMultiviewRenderRequest(File fragmentShader, File outputDirectory);

    RenderRequestBuilder buildMultiviewRetargetRenderRequest(File fragmentShader, File outputDirectory, File retargetViewSet);
}
