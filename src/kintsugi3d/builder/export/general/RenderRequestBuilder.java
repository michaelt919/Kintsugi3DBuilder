/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.general;

import java.io.File;
import java.util.function.Consumer;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.builder.core.IBRRequest;

public interface RenderRequestBuilder<ContextType extends Context<ContextType>>
{
    RenderRequestBuilder<ContextType> useTextureSpaceVertexShader();
    RenderRequestBuilder<ContextType> useCameraSpaceVertexShader();
    RenderRequestBuilder<ContextType> useCustomVertexShader(File vertexShader);

    /**
     * Define a callback function that will be called to set up the shaders immediately before rendering.
     * @param shaderSetupCallback A function to be called to set up a shader program immediately before rendering.
     * @return Reference to the same builder.
     */
    RenderRequestBuilder<ContextType> setShaderSetupCallback(Consumer<Program<ContextType>> shaderSetupCallback);

    IBRRequest<ContextType> create();
    RenderRequestBuilder<ContextType> setWidth(int width);
    RenderRequestBuilder<ContextType> setHeight(int height);
}