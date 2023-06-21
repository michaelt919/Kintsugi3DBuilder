/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import java.io.FileNotFoundException;
import java.util.List;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.geometry.GeometryResources;
import tetzlaff.gl.material.MaterialResources;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.util.ColorList;

public interface IBRResources<ContextType extends Context<ContextType>> extends Resource
{
    /**
     * The graphics context associated with this instance.
     */
    ContextType getContext();

    /**
     * The view set that these resources were loaded from.
     */
    ViewSet getViewSet();

    /**
     * Gets the weight associated with a given view/camera (determined by the distance from other views).
     * @param index The index of the view for which to retrieve its weight.
     * @return The weight for the specified view.
     */
    float getCameraWeight(int index);


    /**
     * Gets a read-only view of the whole list of camera weights
     * @return
     */
    List<Float> getCameraWeights();

    GeometryResources<ContextType> getGeometryResources();

    /**
     * Diffuse, normal, specular, roughness maps
     * @return
     */
    MaterialResources<ContextType> getMaterialResources();

    /**
     * 1D textures for encoding and decoding
     * @return
     */
    LuminanceMapResources<ContextType> getLuminanceMapResources();

    /**
     * Refresh the luminance map textures using the current values in the view set.
     */
    void updateLuminanceMap();

    /**
     * Gets a shader program builder with any required preprocessor defines automatically injected based on the
     * characteristics of this instance.
     * @param renderingMode The rendering mode to use, which may change some of the preprocessor defines.
     * @return A program builder with preprocessor defines specified, ready to have the vertex and fragment shaders
     * added as well as any additional application-specific preprocessor definitions.
     */
    ProgramBuilder<ContextType> getShaderProgramBuilder(StandardRenderingMode renderingMode);


    /**
     * Gets a shader program builder with preprocessor defines automatically injected based on the
     * characteristics of this instance.
     * This overload uses the default mode of RenderingMode.IMAGE_BASED.
     * @return A program builder with preprocessor defines specified, ready to have the vertex and fragment shaders
     * added as well as any additional application-specific preprocessor definitions.
     */
    default ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        return getShaderProgramBuilder(StandardRenderingMode.IMAGE_BASED);
    }

    /**
     * Sets up a shader program to use this instance's IBR resources.
     * While the geometry is generally associated with a Drawable using the createDrawable function,
     * this method binds all of the textures and associated data like camera poses, light positions, etc.
     * to the shader program's uniform variables.
     * @param program The shader program to set up using this instance's resources.
     */
    void setupShaderProgram(Program<ContextType> program);

    /**
     * Creates a Drawable using this instance's geometry resources, and the specified shader program.
     * @param program The program to use to construct the Drawable.
     * @return A Drawable for rendering this instance using the specified shader program.
     */
    Drawable<ContextType> createDrawable(Program<ContextType> program);

    default GraphicsStreamFactory<ContextType> streamFactory()
    {
        return new GraphicsStreamFactory<>(this);
    }
}
