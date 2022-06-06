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

package tetzlaff.gl.core;

/**
 * Enumerates the possible types of shaders.
 * @author Michael Tetzlaff
 *
 */
public enum ShaderType 
{
    /**
     * A shader that processes individual vertices using vertex attributes stored in vertex buffers.
     * This is generally the first stage of the GL pipeline, and must exist in every shader program.
     */
    VERTEX,

    /**
     * A shader that processes individual fragments, or pixels, and produces colors and depth.
     * This is generally the last stage of the GL pipeline, and must exist in every shader program.
     */
    FRAGMENT,

    /**
     * A shader that governs the processing of primitives.
     * For more information, see: https://www.opengl.org/wiki/Geometry_Shader
     */
    GEOMETRY,

    /**
     * A shader used for tessellation that controls how much tessellation occurs.
     * For more information, see: https://www.opengl.org/wiki/Tessellation_Control_Shader
     */
    TESSELATION_CONTROL,

    /**
     * A shader used for tesselation that evaluates interpolated per-vertex data.
     * For more information, see: https://www.opengl.org/wiki/Tessellation_Evaluation_Shader
     */
    TESSELATION_EVALUATION,

    /**
     * A shader for arbitrary, general-purpose computation.
     * For more information, see: https://www.opengl.org/wiki/Compute_Shader
     */
    COMPUTE
}
