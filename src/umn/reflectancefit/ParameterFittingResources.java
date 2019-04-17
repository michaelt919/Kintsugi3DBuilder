/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.reflectancefit;

import umn.gl.core.Context;
import umn.gl.core.Framebuffer;
import umn.gl.core.Program;
import umn.gl.core.Texture3D;

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
