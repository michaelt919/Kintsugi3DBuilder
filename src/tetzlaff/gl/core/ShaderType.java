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
