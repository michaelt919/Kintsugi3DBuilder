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

package kintsugi3d.gl.core;

public interface ContextState
{
    /**
     * Enables depth testing for this context.
     */
    void enableDepthTest();

    /**
     * Disables depth testing for this context.
     */
    void disableDepthTest();

    /**
     * Enables writing to the depth buffer for this context (but makes no change to the state of the depth test itself).
     */
    void enableDepthWrite();

    /**
     * Disables writing to the depth buffer for this context (but makes no change to the state of the depth test itself).
     */
    void disableDepthWrite();

    /**
     * Enables multisampling for this context.
     */
    void enableMultisampling();

    /**
     * Disables multisampling for this context.
     */
    void disableMultisampling();

    /**
     * Enables back-face culling for this context.
     */
    void enableBackFaceCulling();

    /**
     * Disables back-face culling for this context.
     */
    void disableBackFaceCulling();

    /**
     * Enables blending and sets the blending function to be used for this context.
     * @param func The blending function to be used.
     */
    void setBlendFunction(BlendFunction func);

    /**
     * Disables blending for this context.
     */
    void disableBlending();

    /**
     * Gets the maximum number of words allowed across all vertex shader uniform blocks.
     * @return The maximum number of words allowed across all vertex shader uniform blocks.
     */
    int getMaxCombinedVertexUniformComponents();

    /**
     * Gets the maximum number of words allowed across all fragment shader uniform blocks.
     * @return The maximum number of words allowed across all fragment shader uniform blocks.
     */
    int getMaxCombinedFragmentUniformComponents();

    /**
     * Gets the maximum size of a uniform block.
     * @return The maximum size of a uniform block.
     */
    int getMaxUniformBlockSize();

    /**
     * Gets the maximum number of uniform components allowed in a vertex shader.
     * @return The maximum number of uniform components allowed in a vertex shader.
     */
    int getMaxVertexUniformComponents();

    /**
     * Gets the maximum number of uniform components allowed in a fragment shader.
     * @return The maximum number of uniform components allowed in a fragment shader.
     */
    int getMaxFragmentUniformComponents();

    /**
     * Gets the maximum number of layers allowed in a texture array.
     * @return The maximum number of layers allowed in a texture array.
     */
    int getMaxArrayTextureLayers();

    /**
     * Gets the maximum number of textures allowed.
     * @return The maximum number of textures allowed.
     */
    int getMaxCombinedTextureImageUnits();

    /**
     * Gets the maximum number of uniform blocks allowed.
     * @return Gets the maximum number of uniform blocks allowed.
     */
    int getMaxCombinedUniformBlocks();
}
