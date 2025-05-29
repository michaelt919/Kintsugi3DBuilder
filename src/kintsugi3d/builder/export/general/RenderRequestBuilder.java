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

import java.io.File;
import java.util.function.Consumer;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.builder.core.ObservableIBRRequest;

public interface RenderRequestBuilder
{
    RenderRequestBuilder useTextureSpaceVertexShader();
    RenderRequestBuilder useCameraSpaceVertexShader();
    RenderRequestBuilder useCustomVertexShader(File vertexShader);

    /**
     * Define a callback function that will be called to set up the shaders immediately before rendering.
     * @param shaderSetupCallback A function to be called to set up a shader program immediately before rendering.
     * @return Reference to the same builder.
     */
    RenderRequestBuilder setShaderSetupCallback(Consumer<Program<? extends Context<?>>> shaderSetupCallback);

    ObservableIBRRequest create();
    RenderRequestBuilder setWidth(int width);
    RenderRequestBuilder setHeight(int height);
}
