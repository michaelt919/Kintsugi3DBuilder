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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;

/**
 * An interface for any OpenGL-like graphics context.
 * An implementation of this interface serves several purposes:
 * (1) it provides query functions for obtaining information about the GL state, and
 * (2) it serves as an access point for modifying global GL options like depth testing, multisampling, and back-face culling,
 * (3) it provides synchronization functions like flush(), finish(), and swapBuffers(),
 * (4) it acts as a factory for creating GL objects such as buffers, textures, and shaders.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> For concrete types, this type parameter should match the implementing class itself.
 * Abstract implementations should be parameterized so that subclasses can fulfill this constraint.
 * This type parameter ensures that all the objects created by this context are mutually compatible with each other and the context itself.
 */
public interface Context<ContextType extends Context<ContextType>> extends AutoCloseable
{
    /**
     * Designates this context as the "current" context that should be used for all subsequent GL operations.
     * Currently, in a multi-context environment, it would be necessary to call this before using any objects created by this context.
     * Future revisions may implement the automatic switching of contexts when necessary, in which case this method will be eliminated.
     */
    void makeContextCurrent();

    /**
     * Flushes all graphics commands for this context.
     */
    void flush();

    /**
     * Waits for this context to finish all pending graphics commands.
     */
    void finish();

    /**
     * Destroys this context.
     * This should free any GPU resources previously allocated to this context.
     * After this method has been called, all other methods will have undefined results.
     */
    @Override
    void close();

    /**
     * Gets an object that represents the state of the graphics context and provides methods for manipulating that state.
     * @return
     */
    ContextState getState();

    /**
     * Gets the default framebuffer.
     * @return The default framebuffer.
     */
    DoubleFramebuffer<ContextType> getDefaultFramebuffer();

    /**
     * Creates a new shader from a string containing the source code.
     * @param type The type of shader to create.
     * @param source The source code of the shader.
     * @return The newly created shader.
     */
    Shader<ContextType> createShader(ShaderType type, String source);

    /**
     * Creates a new shader from a file containing the source code and a set of preprocessor #defines to be injected into the code.
     * @param type The type of shader to create.
     * @param file A file containing the source code of the shader.
     * @param defines A map from every #define (by name) to the value that should be assigned to that name in the shader.
     *                These defines should be injected at the very beginning of the shader, immediately after the #version statement (in GLSL).
     * @return The newly created shader.
     * @throws FileNotFoundException Upon a File I/O problem when reading the shader file.
     */
    Shader<ContextType> createShader(ShaderType type, File file, Map<String, Object> defines) throws FileNotFoundException;

    /**
     * Creates a new shader from a file containing the source code.
     * @param type The type of shader to create.
     * @param file A file containing the source code of the shader.
     * @return The newly created shader.
     * @throws FileNotFoundException Upon a File I/O problem when reading the shader file.
     */
    default Shader<ContextType> createShader(ShaderType type, File file) throws FileNotFoundException
    {
        return createShader(type, file, Collections.emptyMap());
    }

    /**
     * Gets a builder object for a shader program.
     * @return A builder object for a shader program.
     */
    ProgramBuilder<ContextType> getShaderProgramBuilder();

    /**
     * Gets a builder for a framebuffer object.
     * @param width The width of the framebuffer.
     * @param height The height of the framebuffer.
     * @return The builder for a framebuffer object with the specified dimensions.
     */
    FramebufferObjectBuilder<ContextType> buildFramebufferObject(int width, int height);

    /**
     * Creates a new vertex buffer.
     * @return The newly created vertex buffer.
     */
    VertexBuffer<ContextType> createVertexBuffer();

    /**
     * Creates a vertex buffer containing 4 vertices that can be rendered as a triangle fan to form a rectangle from [-1, -1] to [1, 1].
     * @return The newly created vertex buffer for drawing a rectangle.
     */
    default VertexBuffer<ContextType> createRectangle()
    {
        return this.createVertexBuffer().setData(NativeVectorBufferFactory.getInstance().createFromFloatArray(2, 4, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f), false);
    }

    /**
     * Creates a new index buffer.
     * @return The newly created index buffer.
     */
    IndexBuffer<ContextType> createIndexBuffer();

    /**
     * Creates a new uniform buffer.
     * @return The newly created uniform buffer.
     */
    UniformBuffer<ContextType> createUniformBuffer();

    /**
     * Creates a new drawable object.
     * @param program The shader program to use with the drawable object.
     * @return The newly created drawable object.
     */
    Drawable<ContextType> createDrawable(Program<ContextType> program);

    /**
     * Gets an factory object for the purpose of creating texture resources on the GPU.
     * @return The texture factory
     */
    TextureFactory<ContextType> getTextureFactory();
}
