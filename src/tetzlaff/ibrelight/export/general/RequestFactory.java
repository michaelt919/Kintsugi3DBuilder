/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.general;

import java.io.File;

public interface RequestFactory
{
    RenderRequestBuilder buildSingleFrameRenderRequest(File fragmentShader, File outputDirectory, String outputImageName);
    RenderRequestBuilder buildMultiframeRenderRequest(File fragmentShader, File outputDirectory, int frameCount);
    RenderRequestBuilder buildMultiviewRenderRequest(File fragmentShader, File outputDirectory);
    RenderRequestBuilder buildMultiviewRetargetRenderRequest(File fragmentShader, File outputDirectory, File retargetViewSet);
}
