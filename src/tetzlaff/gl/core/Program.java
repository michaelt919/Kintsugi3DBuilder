/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

import java.util.Optional;

import tetzlaff.gl.vecmath.*;

/**
 * An interface for a program that can be used for rendering.
 * A program's behavior can be modified dynamically by changing "uniform" variables which remain constant with respect to a single draw call.
 * These variables can be scalars, vectors, uniform buffer objects (which consist of multiple scalars or vectors stored in a single buffer), or texture objects.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the program is associated with.
 */
public interface Program<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
    boolean hasDefine(String key);

    Optional<Object> getDefine(String key);

    boolean setUniform(String name, boolean value);

    boolean setUniform(String name, Vector4 value);

    boolean setUniform(String name, Vector3 value);

    boolean setUniform(String name, Vector2 value);

    boolean setUniform(String name, float value);

    boolean setUniform(String name, IntVector4 value);

    boolean setUniform(String name, IntVector3 value);

    boolean setUniform(String name, IntVector2 value);

    boolean setUniform(String name, int value);

    boolean setUniform(String name, Matrix4 value);

    boolean setUniform(int location, boolean value);

    boolean setUniform(int location, Vector4 value);

    boolean setUniform(int location, Vector3 value);

    boolean setUniform(int location, Vector2 value);

    boolean setUniform(int location, float value);

    boolean setUniform(int location, IntVector4 value);

    boolean setUniform(int location, IntVector3 value);

    boolean setUniform(int location, IntVector2 value);

    boolean setUniform(int location, int value);

    boolean setUniform(int location, Matrix4 value);

    int getUniformLocation(String name);

    int getVertexAttribLocation(String name);

    boolean setTexture(int location, Texture<ContextType> texture);

    boolean setTexture(String name, Texture<ContextType> texture);

    boolean setUniformBuffer(int index, UniformBuffer<ContextType> buffer);

    boolean setUniformBuffer(String name, UniformBuffer<ContextType> buffer);

    int getUniformBlockIndex(String name);

}