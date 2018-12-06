/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.opengl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import umn.gl.builders.ProgramBuilder;
import umn.gl.builders.framebuffer.FramebufferObjectBuilder;
import umn.gl.core.*;
import umn.gl.exceptions.*;
import umn.gl.glfw.WindowContextBase;
import umn.gl.nativebuffer.NativeDataType;
import umn.gl.opengl.OpenGLFramebufferObject.OpenGLFramebufferObjectBuilder;
import umn.gl.opengl.OpenGLProgram.OpenGLProgramBuilder;
import umn.gl.types.AbstractDataType;
import umn.gl.types.PackedDataType;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.EXTTextureSRGB.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL43.*;

public class OpenGLContext extends WindowContextBase<OpenGLContext>
{
    private final OpenGLContextState state;
    private final OpenGLTextureFactory textureFactory;

    private final Map<Integer, OpenGLTexture> textureBindings;
    private final Map<Integer, OpenGLUniformBuffer> uniformBufferBindings;

    private DoubleFramebuffer<OpenGLContext> defaultFramebuffer;

    OpenGLContext(long handle)
    {
        super(handle);
        this.state = new OpenGLContextState(this);
        this.textureFactory = new OpenGLTextureFactory(this);
        this.textureBindings = new HashMap<>(48);
        this.uniformBufferBindings = new HashMap<>(24);
    }

    @Override
    public void flush()
    {
        glFlush();
        errorCheck();
    }

    @Override
    public void finish()
    {
        glFinish();
        errorCheck();
    }

    @Override
    public OpenGLContextState getState()
    {
        return this.state;
    }

    @Override
    public Shader<OpenGLContext> createShader(ShaderType type, String source)
    {
        return new OpenGLShader(this, getOpenGLShaderType(type), source);
    }

    @Override
    public Shader<OpenGLContext> createShader(ShaderType type, File file, Map<String, Object> defines) throws FileNotFoundException
    {
        return new OpenGLShader(this, getOpenGLShaderType(type), file, defines);
    }

    @Override
    public ProgramBuilder<OpenGLContext> getShaderProgramBuilder()
    {
        return new OpenGLProgramBuilder(this);
    }

    @Override
    public DoubleFramebuffer<OpenGLContext> getDefaultFramebuffer()
    {
        return this.defaultFramebuffer;
    }

    void setDefaultFramebuffer(DoubleFramebuffer<OpenGLContext> defaultFramebuffer)
    {
        this.defaultFramebuffer = defaultFramebuffer;
    }

    @Override
    public VertexBuffer<OpenGLContext> createVertexBuffer()
    {
        return new OpenGLVertexBuffer(this);
    }

    @Override
    public IndexBuffer<OpenGLContext> createIndexBuffer()
    {
        return new OpenGLIndexBuffer(this);
    }

    @Override
    public UniformBuffer<OpenGLContext> createUniformBuffer()
    {
        return new OpenGLUniformBuffer(this);
    }

    @Override
    public Drawable<OpenGLContext> createDrawable(Program<OpenGLContext> program)
    {
        if (program instanceof OpenGLProgram)
        {
            return new OpenGLDrawable(this, (OpenGLProgram)program);
        }
        else
        {
            throw new IllegalArgumentException("'program' must be of type OpenGLProgram.");
        }
    }

    @Override
    public FramebufferObjectBuilder<OpenGLContext> buildFramebufferObject(int width, int height)
    {
        return new OpenGLFramebufferObjectBuilder(this, width, height);
    }

    @Override
    public TextureFactory<OpenGLContext> getTextureFactory()
    {
        return this.textureFactory;
    }

    static int getPixelDataFormatFromDimensions(int dimensions, boolean integer)
    {
        if (integer)
        {
            switch (dimensions)
            {
                case 1:
                    return GL_RED_INTEGER;
                case 2:
                    return GL_RG_INTEGER;
                case 3:
                    return GL_RGB_INTEGER;
                case 4:
                    return GL_RGBA_INTEGER;
                default:
                    throw new IllegalArgumentException("Data must be a vertex list of no more than 4 dimensions.");
            }
        }
        else
        {
            switch (dimensions)
            {
                case 1:
                    return GL_RED;
                case 2:
                    return GL_RG;
                case 3:
                    return GL_RGB;
                case 4:
                    return GL_RGBA;
                default:
                    throw new IllegalArgumentException("Data must be a vertex list of no more than 4 dimensions.");
            }
        }
    }

    static int getDataTypeConstant(NativeDataType dataType)
    {
        switch(dataType)
        {
        case UNSIGNED_BYTE: return GL_UNSIGNED_BYTE;
        case BYTE: return GL_BYTE;
        case UNSIGNED_SHORT: return GL_UNSIGNED_SHORT;
        case SHORT: return GL_SHORT;
        case UNSIGNED_INT: return GL_UNSIGNED_INT;
        case INT: return GL_INT;
        case FLOAT: return GL_FLOAT;
        case DOUBLE: return GL_DOUBLE;
        case PACKED_BYTE:
        case PACKED_SHORT:
        case PACKED_INT:
            throw new IllegalArgumentException("Packed native data type specified outside the context of a packing scheme.");
        default:
            throw new IllegalArgumentException("Unrecognized data type."); // Shouldn't ever happen
        }
    }

    static int getDataTypeConstant(AbstractDataType<?> dataType)
    {
        if (dataType instanceof PackedDataType)
        {
            switch((PackedDataType)dataType)
            {
                case BYTE_3_3_2:        return GL_UNSIGNED_BYTE_3_3_2;
                case SHORT_5_6_5:       return GL_UNSIGNED_SHORT_5_6_5;
                case SHORT_5_5_5_1:     return GL_UNSIGNED_SHORT_5_5_5_1;
                case SHORT_4_4_4_4:     return GL_UNSIGNED_SHORT_4_4_4_4;
                case INT_10_10_10_2:    return GL_UNSIGNED_INT_10_10_10_2;
                case INT_8_8_8_8:       return GL_UNSIGNED_INT_8_8_8_8;
                default:
                    throw new IllegalArgumentException("Unrecognized packed data type."); // Shouldn't ever happen
            }
        }
        else
        {
            return getDataTypeConstant(dataType.getNativeDataType());
        }
    }

    void bindTextureToUnit(int textureUnitIndex, OpenGLTexture texture)
    {
        glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
        errorCheck();

        glBindTexture(texture.getOpenGLTextureTarget(), texture.getTextureId());
        errorCheck();

        textureBindings.put(textureUnitIndex, texture);
    }

    void bindUniformBufferToIndex(int bufferBindingIndex, OpenGLUniformBuffer buffer)
    {
        glBindBufferBase(GL_UNIFORM_BUFFER, bufferBindingIndex, buffer.getBufferId());
        errorCheck();

        uniformBufferBindings.put(bufferBindingIndex, buffer);
    }

    private static <T> void updateBindingsGeneric(Map<Integer, T> oldBindings, List<Optional<T>> newBindings,
        BiPredicate<T, T> needsUnbindPredicate, BiConsumer<Integer, T> unbindFunction, BiConsumer<Integer, T> bindFunction)
    {
        // Iterate through all of the previous bindings and unbind any that will not be overwritten otherwise.
        Iterator<Entry<Integer, T>> iterator = oldBindings.entrySet().iterator();
        while (iterator.hasNext())
        {
            Entry<Integer, T> oldBinding = iterator.next();
            int bindingIndex = oldBinding.getKey();
            T objectToUnbind = oldBinding.getValue();
            Optional<? extends T> newObjectToBind = newBindings.get(bindingIndex);

            if (!newObjectToBind.isPresent() || needsUnbindPredicate.test(objectToUnbind, newObjectToBind.get()))
            {
                unbindFunction.accept(bindingIndex, objectToUnbind);
                iterator.remove();
            }
        }

        for (int i = 0; i < newBindings.size(); i++)
        {
            if (newBindings.get(i).isPresent())
            {
                T newObjectToBind = newBindings.get(i).get();

                if (!oldBindings.containsKey(i) || !Objects.equals(oldBindings.get(i), newObjectToBind))
                {
                    bindFunction.accept(i, newObjectToBind);
                }
            }
        }
    }

    void updateTextureBindings(List<Optional<OpenGLTexture>> newTextureBindings)
    {
        updateBindingsGeneric(textureBindings, newTextureBindings,
            (oldTexture, newTexture) -> oldTexture.getOpenGLTextureTarget() != newTexture.getOpenGLTextureTarget(),
            (textureUnitIndex, textureToUnbind) ->
            {
                glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
                errorCheck();

                glBindTexture(textureToUnbind.getOpenGLTextureTarget(), 0);
                errorCheck();
            },
            (textureUnitIndex, textureToBind) -> textureToBind.bindToTextureUnit(textureUnitIndex));
    }

    void updateUniformBufferBindings(List<Optional<OpenGLUniformBuffer>> newUniformBufferBindings)
    {
        updateBindingsGeneric(uniformBufferBindings, newUniformBufferBindings,
            (oldBuffer, newBuffer) -> oldBuffer.getBufferTarget() != newBuffer.getBufferTarget(),
            (bindingIndex, bufferToUnbind) ->
            {
                glBindBufferBase(bufferToUnbind.getBufferTarget(), bindingIndex, 0);
                errorCheck();
            },
            (bindingIndex, bufferToBind) -> bufferToBind.bindToIndex(bindingIndex));
    }

    protected static int getOpenGLInternalColorFormat(ColorFormat format)
    {
        if (format.alphaBits > 0)
        {
            switch(format.dataType)
            {
            case FLOATING_POINT:
                if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16 && format.alphaBits <= 16)
                {
                    return GL_RGBA16F;
                }
                else
                {
                    return GL_RGBA32F;
                }
            case UNSIGNED_INTEGER:
                if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
                {
                    return GL_RGBA8UI;
                }
                else if (format.redBits <= 10 && format.greenBits <= 10 && format.blueBits <= 10 && format.alphaBits <= 2)
                {
                    return GL_RGB10_A2UI;
                }
                else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16 && format.alphaBits <= 16)
                {
                    return GL_RGBA16UI;
                }
                else
                {
                    return GL_RGBA32UI;
                }
            case SIGNED_INTEGER:
                if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
                {
                    return GL_RGBA8I;
                }
                else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16 && format.alphaBits <= 16)
                {
                    return GL_RGBA16I;
                }
                else
                {
                    return GL_RGBA32I;
                }
            case SRGB_FIXED_POINT:
                return GL_SRGB8_ALPHA8;
            case SIGNED_FIXED_POINT:
                if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
                {
                    return GL_RGBA8_SNORM;
                }
                else
                {
                    return GL_RGBA16_SNORM;
                }
            case NORMALIZED_FIXED_POINT:
            default:
                if (format.redBits <= 2 && format.greenBits <= 2 && format.blueBits <= 2 && format.alphaBits <= 2)
                {
                    return GL_RGBA2;
                }
                else if (format.redBits <= 4 && format.greenBits <= 4 && format.blueBits <= 4 && format.alphaBits <= 4)
                {
                    return GL_RGBA4;
                }
                else if (format.redBits <= 5 && format.greenBits <= 5 && format.blueBits <= 5 && format.alphaBits <= 1)
                {
                    return GL_RGB5_A1;
                }
                else if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
                {
                    return GL_RGBA8;
                }
                else if (format.redBits <= 10 || format.greenBits <= 10 || format.blueBits <= 10 || format.alphaBits <= 2)
                {
                    return GL_RGB10_A2;
                }
                else if (format.redBits <= 12 || format.greenBits <= 12 || format.blueBits <= 12 || format.alphaBits <= 12)
                {
                    return GL_RGBA12;
                }
                else
                {
                    return GL_RGBA16;
                }
            }
        }
        else if (format.blueBits > 0)
        {
            switch(format.dataType)
            {
            case FLOATING_POINT:
                if (format.redBits <= 11 && format.greenBits <= 11 && format.blueBits <= 10)
                {
                    return GL_R11F_G11F_B10F;
                }
                else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16)
                {
                    return GL_RGB16F;
                }
                else
                {
                    return GL_RGB32F;
                }
            case UNSIGNED_INTEGER:
                if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
                {
                    return GL_RGB8UI;
                }
                else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16)
                {
                    return GL_RGB16UI;
                }
                else
                {
                    return GL_RGB32UI;
                }
            case SIGNED_INTEGER:
                if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
                {
                    return GL_RGB8I;
                }
                else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16)
                {
                    return GL_RGB16I;
                }
                else
                {
                    return GL_RGB32I;
                }
            case SRGB_FIXED_POINT:
                return GL_SRGB8;
            case SIGNED_FIXED_POINT:
                if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
                {
                    return GL_RGB8_SNORM;
                }
                else
                {
                    return GL_RGB16_SNORM;
                }
            case NORMALIZED_FIXED_POINT:
            default:
                if (format.redBits <= 3 && format.greenBits <= 3 && format.blueBits <= 2)
                {
                    return GL_R3_G3_B2;
                }
                else if (format.redBits <= 4 && format.greenBits <= 4 && format.blueBits <= 4)
                {
                    return GL_RGB4;
                }
                else if (format.redBits <= 5 && format.greenBits <= 5 && format.blueBits <= 5)
                {
                    return GL_RGB5;
                }
                else if (format.redBits <= 5 && format.greenBits <= 6 && format.blueBits <= 5)
                {
                    return GL_RGB565;
                }
                else if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
                {
                    return GL_RGB8;
                }
                else if (format.redBits <= 10 || format.greenBits <= 10 || format.blueBits <= 10)
                {
                    return GL_RGB10;
                }
                else if (format.redBits <= 12 || format.greenBits <= 12 || format.blueBits <= 12)
                {
                    return GL_RGB12;
                }
                else
                {
                    return GL_RGB16;
                }
            }
        }
        else if (format.greenBits > 0)
        {
            switch(format.dataType)
            {
            case FLOATING_POINT:
                if (format.redBits <= 16 && format.greenBits <= 16)
                {
                    return GL_RG16F;
                }
                else
                {
                    return GL_RG32F;
                }
            case UNSIGNED_INTEGER:
                if (format.redBits <= 8 && format.greenBits <= 8)
                {
                    return GL_RG8UI;
                }
                else if (format.redBits <= 16 && format.greenBits <= 16)
                {
                    return GL_RG16UI;
                }
                else
                {
                    return GL_RG32UI;
                }
            case SIGNED_INTEGER:
                if (format.redBits <= 8 && format.greenBits <= 8)
                {
                    return GL_RG8I;
                }
                else if (format.redBits <= 16 && format.greenBits <= 16)
                {
                    return GL_RG16I;
                }
                else
                {
                    return GL_RG32I;
                }
            case SRGB_FIXED_POINT:
                return GL_SRGB8;
            case SIGNED_FIXED_POINT:
                if (format.redBits <= 8 && format.greenBits <= 8)
                {
                    return GL_RG8_SNORM;
                }
                else
                {
                    return GL_RG16_SNORM;
                }
            case NORMALIZED_FIXED_POINT:
            default:
                if (format.redBits <= 8 && format.greenBits <= 8)
                {
                    return GL_RG8;
                }
                else
                {
                    return GL_RG16;
                }
            }
        }
        else
        {
            switch(format.dataType)
            {
            case FLOATING_POINT:
                if (format.redBits <= 16)
                {
                    return GL_R16F;
                }
                else
                {
                    return GL_R32F;
                }
            case UNSIGNED_INTEGER:
                if (format.redBits <= 8)
                {
                    return GL_R8UI;
                }
                else if (format.redBits <= 16)
                {
                    return GL_R16UI;
                }
                else
                {
                    return GL_R32UI;
                }
            case SIGNED_INTEGER:
                if (format.redBits <= 8)
                {
                    return GL_R8I;
                }
                else if (format.redBits <= 16)
                {
                    return GL_R16I;
                }
                else
                {
                    return GL_R32I;
                }
            case SRGB_FIXED_POINT:
                return GL_SRGB8;
            case SIGNED_FIXED_POINT:
                if (format.redBits <= 8)
                {
                    return GL_R8_SNORM;
                }
                else
                {
                    return GL_R16_SNORM;
                }
            case NORMALIZED_FIXED_POINT:
            default:
                if (format.redBits <= 8)
                {
                    return GL_R8;
                }
                else
                {
                    return GL_R16;
                }
            }
        }
    }

    protected static int getOpenGLCompressionFormat(CompressionFormat format)
    {
        switch(format)
        {
        case RED_4BPP: return GL_COMPRESSED_RED_RGTC1;
        case SIGNED_RED_4BPP: return GL_COMPRESSED_SIGNED_RED_RGTC1;
        case RED_4BPP_GREEN_4BPP: return GL_COMPRESSED_RG_RGTC2;
        case SIGNED_RED_4BPP_GREEN_4BPP: return GL_COMPRESSED_SIGNED_RG_RGTC2;
        case RGB_4BPP: return GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
        case SRGB_4BPP: return GL_COMPRESSED_SRGB_S3TC_DXT1_EXT;
        case RGB_PUNCHTHROUGH_ALPHA1_4BPP: return GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
        case SRGB_PUNCHTHROUGH_ALPHA1_4BPP: return GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT;
        case RGB_4BPP_ALPHA_4BPP: return GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
        case SRGB_4BPP_ALPHA_4BPP: return GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT;
        default: throw new IllegalArgumentException("Unsupported compression format.");
        }
    }

    protected static int getOpenGLInternalDepthFormat(int precision)
    {
        if (precision <= 16)
        {
            return GL_DEPTH_COMPONENT16;
        }
        else if (precision <= 24)
        {
            return GL_DEPTH_COMPONENT24;
        }
        else
        {
            return GL_DEPTH_COMPONENT32;
        }
    }

    protected static int getOpenGLInternalStencilFormat(int precision)
    {
        if (precision == 1)
        {
            return GL_STENCIL_INDEX1;
        }
        if (precision <= 4)
        {
            return GL_STENCIL_INDEX4;
        }
        else if (precision <= 8)
        {
            return GL_STENCIL_INDEX8;
        }
        else
        {
            return GL_STENCIL_INDEX16;
        }
    }

    protected static int getOpenGLShaderType(ShaderType type)
    {
        switch(type)
        {
        case VERTEX: return GL_VERTEX_SHADER;
        case FRAGMENT: return GL_FRAGMENT_SHADER;
        case GEOMETRY: return GL_GEOMETRY_SHADER;
        case TESSELATION_CONTROL: return GL_TESS_CONTROL_SHADER;
        case TESSELATION_EVALUATION: return GL_TESS_EVALUATION_SHADER;
        case COMPUTE: return GL_COMPUTE_SHADER;
        }

        return 0;
    }

    protected static int getOpenGLBufferUsage(BufferAccessType accessType, BufferAccessFrequency accessFreq)
    {
        switch(accessFreq)
        {
        case STREAM:
            switch(accessType)
            {
            case DRAW: return GL_STREAM_DRAW;
            case READ: return GL_STREAM_READ;
            case COPY: return GL_STREAM_COPY;
            }
        case STATIC:
            switch(accessType)
            {
            case DRAW: return GL_STATIC_DRAW;
            case READ: return GL_STATIC_READ;
            case COPY: return GL_STATIC_COPY;
            }
        case DYNAMIC:
            switch(accessType)
            {
            case DRAW: return GL_DYNAMIC_DRAW;
            case READ: return GL_DYNAMIC_READ;
            case COPY: return GL_DYNAMIC_COPY;
            }
        }

        return 0;
    }

    protected static String getFramebufferStatusString(String framebufferName, int statusID)
    {
        switch(statusID)
        {
        case GL_FRAMEBUFFER_COMPLETE: return framebufferName + " is framebuffer complete (no errors).";
        case GL_FRAMEBUFFER_UNDEFINED: return framebufferName + " is the default framebuffer, but the default framebuffer does not exist.";
        case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT: return framebufferName + " has attachment points that are framebuffer incomplete.";
        case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: return framebufferName + " does not have at least one image attached to it.";
        case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
        case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER: return framebufferName + " has a color attachment point without an attached image.";
        case GL_FRAMEBUFFER_UNSUPPORTED: return framebufferName + " has attached images with a combination of internal formats that violates an implementation-dependent set of restrictions.";
        case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE: return framebufferName + " has attachments with different multisample parameters.";
        case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS: return framebufferName + " has a layered attachment and a populated non-layered attachment, or all populated color attachments are not from textures of the same target.";
        case 0: return framebufferName + " has an unknown completeness status because an error has occurred.";
        default: return framebufferName + " has failed an unrecognized completeness check.";
        }
    }

    static void throwInvalidFramebufferOperationException()
    {
        int readStatus = glCheckFramebufferStatus(GL_READ_FRAMEBUFFER);
        int drawStatus = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
        throw new GLInvalidFramebufferOperationException("The framebuffer object is not complete:  " +
                    getFramebufferStatusString("The read framebuffer", readStatus) + "  " + getFramebufferStatusString("The draw framebuffer", drawStatus));
    }

    /**
     * Should always be called after any OpenGL function
     * Search for missing calls to this using this regex:
     * gl[A-Z].*\(.*\);\s*[^\s(OpenGLContext.errorCheck\(\);)]
     */
    protected static void errorCheck()
    {
        int error = glGetError();
        switch (error)
        {
        case GL_NO_ERROR: return;
        case GL_INVALID_ENUM: throw new GLInvalidEnumException();
        case GL_INVALID_VALUE: throw new GLInvalidValueException();
        case GL_INVALID_OPERATION: throw new GLInvalidOperationException();
        case GL_INVALID_FRAMEBUFFER_OPERATION: throwInvalidFramebufferOperationException(); break;
        case GL_OUT_OF_MEMORY: throw new GLOutOfMemoryException();
        case GL_STACK_UNDERFLOW: throw new GLStackUnderflowException();
        case GL_STACK_OVERFLOW: throw new GLStackOverflowException();
        default: throw new GLException("Unrecognized OpenGL Exception.");
        }
    }
}
