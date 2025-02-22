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

import kintsugi3d.gl.vecmath.*;

import java.util.Optional;

/**
 * An interface for a program that can be used for rendering.
 * A program's behavior can be modified dynamically by changing "uniform" variables which remain constant with respect to a single draw call.
 * These variables can be scalars, vectors, uniform buffer objects (which consist of multiple scalars or vectors stored in a single buffer), or texture objects.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the program is associated with.
 */
public interface Program<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{
    /**
     * Checks whether a given preprocessor #define has been specified to be injected into the code.
     * These defines should be injected at the very beginning of the shader, immediately after the #version statement (in GLSL).
     *
     * @param key The name of the #define in the shader.
     * @return True if a #define of that name has been specified for this program; false otherwise.
     */
    boolean hasDefine(String key);

    /**
     * Gets the value of a preprocessor #define has been specified to be injected into the code.
     * These defines should be injected at the very beginning of the shader, immediately after the #version statement (in GLSL).
     *
     * @param key The name of the #define in the shader.
     * @return The value that should be assigned to that name in the shader, if it has been specified.
     */
    Optional<Object> getDefine(String key);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, boolean value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, Vector4 value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, Vector3 value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, Vector2 value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, float value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, IntVector4 value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, IntVector3 value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, IntVector2 value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, int value);

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    default boolean setUniform(String name, IntLike value)
    {
        return setUniform(name, value.getIntValue());
    }

    /**
     * Sets a uniform variable by its shader name.
     *
     * @param name  The name used to reference the variable within the shaders.
     * @param value The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setUniform(String name, Matrix4 value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, boolean value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, Vector4 value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, Vector3 value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, Vector2 value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, float value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, IntVector4 value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, IntVector3 value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, IntVector2 value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, int value);

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    default boolean setUniform(int location, IntLike value)
    {
        return setUniform(location, value.getIntValue());
    }

    /**
     * Sets a uniform variable at a particular location in this shader program.
     *
     * @param location The location of the uniform variable to set.
     * @param value    The value to set the uniform variable to.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setUniform(int location, Matrix4 value);

    /**
     * Gets the location of a uniform variable with a particular name within this shader program.
     *
     * @param name The name used to reference the variable within the shaders.
     * @return The location of the uniform variable.
     */
    int getUniformLocation(String name);

    /**
     * Gets the location of a vertex attribute with a particular name within this shader program.
     *
     * @param name The name used to reference the vertex attribute within the shaders.
     * @return The location of the vertex attribute.
     */
    int getVertexAttribLocation(String name);

    /**
     * Designates a texture to be used as a uniform variable at a particular location in this shader program.
     * A texture slot will automatically be assigned for this texture,
     * and the texture will automatically be loaded into the slot every time this shader program is used.
     *
     * @param location The location of the uniform variable to set.
     * @param texture  The texture to use as this uniform variable.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable exists at the specified location in this shader program.
     */
    boolean setTexture(int location, Texture<ContextType> texture);

    /**
     * Designates a texture to be used as the uniform variable with a particular shader name.
     * A texture slot will automatically be assigned for this texture,
     * and the texture will automatically be loaded into the slot every time this shader program is used.
     *
     * @param name    The name used to reference the variable within the shaders.
     * @param texture The texture to use as this uniform variable.
     * @return true if the uniform variable was successfully set;
     * false if the variable was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setTexture(String name, Texture<ContextType> texture);

    /**
     * Designates a uniform buffer to be used at a particular uniform block index in this shader program.
     * A uniform block slot will automatically be assigned for this uniform block,
     * and the uniform block will automatically be loaded into the slot every time this shader program is used.
     *
     * @param index  The index of the uniform block to set.
     * @param buffer The uniform buffer to use.
     * @return true if the uniform block was successfully set;
     * false if the uniform block was not set because no uniform block exists at the specified index in this shader program.
     */
    boolean setUniformBuffer(int index, UniformBuffer<ContextType> buffer);

    /**
     * Designates a uniform buffer to be used at a particular uniform block index in this shader program.
     * A uniform block slot will automatically be assigned for this uniform block,
     * and the uniform block will automatically be loaded into the slot every time this shader program is used.
     *
     * @param name   The name used to reference the uniform block within the shaders.
     * @param buffer The uniform buffer to use.
     * @return true if the uniform block was successfully set;
     * false if the uniform block was not set because no uniform block with the specified name exists in any of this program's shaders.
     */
    boolean setUniformBuffer(String name, UniformBuffer<ContextType> buffer);

    /**
     * Gets the index of a uniform block with a particular name within this shader program.
     *
     * @param name The name used to reference the uniform block within the shaders.
     * @return The index of the uniform block.
     */
    int getUniformBlockIndex(String name);
}
