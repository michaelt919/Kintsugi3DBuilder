/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.core;

import kintsugi3d.gl.vecmath.*;

/**
 * A drawable entity for a GL context.
 * A "drawable" is essentially a tuple consisting of a program (the instructions) and a set of vertex buffers (the data).
 * It is in some ways analogous to an OpenGL vertex array object (VAO), but it is tied to a specific program 
 * since there is no guarantee that different programs will expect the same set of vertex attributes.
 * Unlike many other GL entities, a Drawable is not a proper "resource;" 
 * any graphics resources that a Drawable does allocate will be automatically deleted when the entity is garbage collected by the JRE.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the Drawable is associated with.
 */
public interface Drawable<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{
    /**
     * Gets the shader program that will be used by this drawable object.
     * @return
     */
    Program<ContextType> program();

    PrimitiveMode getDefaultPrimitiveMode();
    void setDefaultPrimitiveMode(PrimitiveMode primitiveMode);

    /**
     * Draws this to a particular framebuffer using the entire framebuffer as the viewport.
     * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
     * @param framebuffer The framebuffer to render to.
     */
    void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer);

    /**
     * Draws this to a particular framebuffer within a specified viewport rectangle.
     * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
     * @param framebuffer The framebuffer to render to.
     * @param x The number of pixels to the left edge of the viewport rectangle.
     * @param y The number of pixels to the bottom edge of the viewport rectangle.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer, int x, int y,
            int width, int height);

    /**
     * Draws this to a particular framebuffer within a specified viewport rectangle starting at (0, 0).
     * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
     * @param framebuffer The framebuffer to render to.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    void draw(PrimitiveMode primitiveMode, Framebuffer<ContextType> framebuffer, int width,
            int height);

    /**
     * Draws this to the default framebuffer using the entire framebuffer as the viewport.
     * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
     * @param context The context of the default framebuffer to be drawn into.
     */
    void draw(PrimitiveMode primitiveMode, ContextType context);

    /**
     * Draws this to the default framebuffer using the entire framebuffer as the viewport.
     * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
     */
    default void draw(PrimitiveMode primitiveMode)
    {
        this.draw(primitiveMode, getContext());
    }

    /**
     *  Draws this to default framebuffer within a specified viewport rectangle.
     * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
     * @param context The context of the default framebuffer to be drawn into.
     * @param x The number of pixels to the left edge of the viewport rectangle.
     * @param y The number of pixels to the bottom edge of the viewport rectangle.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    void draw(PrimitiveMode primitiveMode, ContextType context, int x, int y, int width, int height);

    /**
     *  Draws this to default framebuffer within a specified viewport rectangle starting at (0, 0).
     * @param primitiveMode What type of primitives to use when interpreting vertex buffers.
     * @param context The context of the default framebuffer to be drawn into.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    void draw(PrimitiveMode primitiveMode, ContextType context, int width, int height);

    /**
     * Draws this (using the default primitive mode) to a particular framebuffer using the entire framebuffer as the viewport.
     * @param framebuffer The framebuffer to render to.
     */
    default void draw(Framebuffer<ContextType> framebuffer)
    {
        this.draw(getDefaultPrimitiveMode(), framebuffer);
    }

    /**
     * Draws this (using the default primitive mode) to a particular framebuffer within a specified viewport rectangle.
     * @param framebuffer The framebuffer to render to.
     * @param x The number of pixels to the left edge of the viewport rectangle.
     * @param y The number of pixels to the bottom edge of the viewport rectangle.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    default void draw(Framebuffer<ContextType> framebuffer, int x, int y, int width, int height)
    {
        this.draw(getDefaultPrimitiveMode(), framebuffer, x, y, width, height);
    }

    /**
     * Draws this (using the default primitive mode) to a particular framebuffer within a specified viewport rectangle starting at (0, 0).
     * @param framebuffer The framebuffer to render to.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    default void draw(Framebuffer<ContextType> framebuffer, int width, int height)
    {
        this.draw(getDefaultPrimitiveMode(), framebuffer, width, height);
    }

    /**
     * Draws this (using the default primitive mode) to the default framebuffer using the entire framebuffer as the viewport.
     * @param context The context of the default framebuffer to be drawn into.
     */
    default void draw(ContextType context)
    {
        this.draw(getDefaultPrimitiveMode(), context);
    }

    /**
     * Draws this (using the default primitive mode) to the default framebuffer using the entire framebuffer as the viewport.
     */
    default void draw()
    {
        this.draw(getDefaultPrimitiveMode());
    }

    /**
     *  Draws this (using the default primitive mode) to default framebuffer within a specified viewport rectangle.
     * @param context The context of the default framebuffer to be drawn into.
     * @param x The number of pixels to the left edge of the viewport rectangle.
     * @param y The number of pixels to the bottom edge of the viewport rectangle.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    default void draw(ContextType context, int x, int y, int width, int height)
    {
        this.draw(getDefaultPrimitiveMode(), context, x, y, width, height);
    }

    /**
     *  Draws this (using the default primitive mode) to default framebuffer within a specified viewport rectangle starting at (0, 0).
     * @param context The context of the default framebuffer to be drawn into.
     * @param width The width of the viewport rectangle in pixels.
     * @param height The height of the viewport rectangle in pixels.
     */
    default void draw(ContextType context, int width, int height)
    {
        this.draw(getDefaultPrimitiveMode(), context, width, height);
    }

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, DoubleVector4 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, DoubleVector3 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, DoubleVector2 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, double value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, Vector4 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, Vector3 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, Vector2 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, float value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, IntVector4 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, IntVector3 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, IntVector2 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    boolean setVertexAttrib(String name, int value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no variable with the specified name exists in any of this program's shaders.
     */
    default boolean setVertexAttrib(String name, IntLike value)
    {
        return setVertexAttrib(name, value.getIntValue());
    }

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, DoubleVector4 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, DoubleVector3 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, DoubleVector2 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, double value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, Vector4 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, Vector3 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, Vector2 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, float value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, IntVector4 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, IntVector3 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, IntVector2 value);

    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean setVertexAttrib(int location, int value);


    /**
     * Designates a specific value for a vertex attribute that should be used for every vertex when this renderable is drawn.
     * This method can be called once and the vertex attribute assignment will persist whenever this renderable is drawn, regardless of changes to the internal GL state.
     * @param location The location of the vertex attribute to set.
     * @param value The value to set the vertex attribute to.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    default boolean setVertexAttrib(int location, IntLike value)
    {
        return setVertexAttrib(location, value.getIntValue());
    }

    /**
     * Designates a vertex buffer to be used for the vertex attribute at a particular location in this renderable's shader program.
     * @param location The location of the vertex attribute to set.
     * @param buffer The vertex buffer to bind to the vertex attribute.
     * @return true if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean addVertexBuffer(int location, VertexBuffer<ContextType> buffer);

    /**
     * Designates a vertex buffer to be used for the vertex attribute with a particular shader name.
     * @param name The name used to reference the vertex attribute within the shaders.
     * @param buffer The vertex buffer to bind to the vertex attribute.
     * @return if the vertex attribute was successfully set;
     * false if the vertex attribute was not set because no vertex attribute exists at the specified location in this shader program.
     */
    boolean addVertexBuffer(String name, VertexBuffer<ContextType> buffer);
}
