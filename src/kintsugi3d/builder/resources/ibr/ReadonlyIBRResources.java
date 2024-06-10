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

package kintsugi3d.builder.resources.ibr;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStreamFactory;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;

public interface ReadonlyIBRResources<ContextType extends Context<ContextType>>
{
    /**
     * The graphics context associated with this instance.
     *  @return The graphics context
     */
    ContextType getContext();

    /**
     * The view set that these resources were loaded from.
     * @return A read-only view of the view set
     */
    ReadonlyViewSet getViewSet();

    /**
     * The geometry used with this instance.
     * @return A read-only view of the geometry
     */
    ReadonlyVertexGeometry getGeometry();

    /**
     * Gets the weight associated with a given view/camera (determined by the distance from other views).
     *
     * @param index The index of the view for which to retrieve its weight.
     * @return The weight for the specified view.
     */
    float getCameraWeight(int index);

    /**
     * Gets a shader program builder with any required preprocessor defines automatically injected based on the
     * characteristics of this instance.
     *
     * @return A program builder with preprocessor defines specified, ready to have the vertex and fragment shaders
     * added as well as any additional application-specific preprocessor definitions.
     */
    ProgramBuilder<ContextType> getShaderProgramBuilder();

    /**
     * Sets up a shader program to use this instance's IBR resources.
     * While the geometry is generally associated with a Drawable using the createDrawable function,
     * this method binds all of the textures and associated data like camera poses, light positions, etc.
     * to the shader program's uniform variables.
     *
     * @param program The shader program to set up using this instance's resources.
     */
    void setupShaderProgram(Program<ContextType> program);

    /**
     * Creates a Drawable using this instance's geometry resources, and the specified shader program.
     *
     * @param program The program to use to construct the Drawable.
     * @return A Drawable for rendering this instance using the specified shader program.
     */
    Drawable<ContextType> createDrawable(Program<ContextType> program);

    GraphicsStreamFactory<ContextType> streamFactory();
}
