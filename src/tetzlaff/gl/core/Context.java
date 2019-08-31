/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
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
    void makeContextCurrent();
    void flush();
    void finish();

    @Override
    void close();

    ContextState getState();

    DoubleFramebuffer<ContextType> getDefaultFramebuffer();

    Shader<ContextType> createShader(ShaderType type, String source);
    Shader<ContextType> createShader(ShaderType type, File file, Map<String, Object> defines) throws FileNotFoundException;

    default Shader<ContextType> createShader(ShaderType type, File file) throws FileNotFoundException
    {
        return createShader(type, file, Collections.emptyMap());
    }

    ProgramBuilder<ContextType> getShaderProgramBuilder();

    FramebufferObjectBuilder<ContextType> buildFramebufferObject(int width, int height);

    VertexBuffer<ContextType> createVertexBuffer();

    default VertexBuffer<ContextType> createRectangle()
    {
        return this.createVertexBuffer().setData(NativeVectorBufferFactory.getInstance().createFromFloatArray(2, 4, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f), false);
    }

    IndexBuffer<ContextType> createIndexBuffer();
    UniformBuffer<ContextType> createUniformBuffer();

    Drawable<ContextType> createDrawable(Program<ContextType> program);

    TextureFactory<ContextType> getTextureFactory();
}
