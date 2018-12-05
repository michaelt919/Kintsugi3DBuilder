package tetzlaff.reflectancefit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.Texture3D;

/**
 * This interface defines methods for retrieving / creating graphics resources that are required by an implementation of the reflectance parameter
 * estimation algorithm.  The interface is also responsible for remembering the names of the material and its enclosing material file, which are
 * generally defined with the geometry and thus are easily obtained at the same time that the geometry is retrieved for transfer to the graphics card.
 * @param <ContextType>
 */
public interface ParameterFittingResources<ContextType extends Context<ContextType>>
{
    /**
     * Creates an object that is capable of running the preliminary diffuse reflectance estimation algorithm.
     * @param framebuffer A framebuffer in which to store the results of the diffuse reflectance estimation.
     * @param subdiv The texture will be divided into this many partitions along each dimension for the purpose of avoiding graphics card timeouts.
     * @return An object that is capable of computing an estimate of the diffuse reflectance.
     */
    DiffuseFit<ContextType> createDiffuseFit(Framebuffer<ContextType> framebuffer, int subdiv);

    /**
     * Creates an object that is capable of running the specular reflectance parameter estimation algorithm.
     * @param framebuffer A framebuffer in which to store the results of the specular reflectance parameter estimation.
     * @param subdiv The texture will be divided into this many partitions along each dimension for the purpose of avoiding graphics card timeouts.
     * @return An object that is capable of computing an estimate of the specular reflectance parameters.
     */
    SpecularFit<ContextType> createSpecularFit(Framebuffer<ContextType> framebuffer, int subdiv);

    /**
     * Creates a texture array containing images with views of the reflecting surface.  Masks are stored in the alpha channel of this texture array.
     * @return The view texture array.
     */
    Texture3D<ContextType> getViewTextures();

    /**
     * Creates a texture array containing depth measurements of the reflecting surface -
     * that is, measurements of the distance from the camera to the reflecting surface.
     * @return The depth texture array.
     */
    Texture3D<ContextType> getDepthTextures();

    /**
     * Creates a shader program that can be used for hole filling.
     * @return A hole filling shader program.
     */
    Program<ContextType> getHoleFillProgram();

    /**
     * Gets the name of any material file discovered when loading the geometry of the reflecting surface.
     * This file is assumed to be associated with the material whose reflectance is to be estimated.
     * @return The name of the material file.
     */
    String getMaterialFileName();

    /**
     * Gets the name of the material whose reflectance is to be estimated.
     * @return The name of the material.
     */
    String getMaterialName();
}
