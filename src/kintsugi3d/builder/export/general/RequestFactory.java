/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.export.general;

import kintsugi3d.gl.core.Context;

import java.io.File;

public interface RequestFactory
{
    <ContextType extends Context<ContextType>>
    RenderRequestBuilder<ContextType> buildSingleFrameRenderRequest(File fragmentShader, File outputDirectory, String outputImageName);

    <ContextType extends Context<ContextType>>
    RenderRequestBuilder<ContextType> buildMultiframeRenderRequest(File fragmentShader, File outputDirectory, int frameCount);

    <ContextType extends Context<ContextType>>
    RenderRequestBuilder<ContextType> buildMultiviewRenderRequest(File fragmentShader, File outputDirectory);

    <ContextType extends Context<ContextType>>
    RenderRequestBuilder<ContextType> buildMultiviewRetargetRenderRequest(File fragmentShader, File outputDirectory, File retargetViewSet);
}
