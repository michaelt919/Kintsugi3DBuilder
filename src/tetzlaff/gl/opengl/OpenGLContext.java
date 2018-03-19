package tetzlaff.gl.opengl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.exceptions.*;
import tetzlaff.gl.glfw.GLFWWindowContextBase;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.opengl.OpenGLFramebufferObject.OpenGLFramebufferObjectBuilder;
import tetzlaff.gl.opengl.OpenGLProgram.OpenGLProgramBuilder;

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

public class OpenGLContext extends GLFWWindowContextBase<OpenGLContext>
{
    private final OpenGLContextState state;
    private final OpenGLTextureFactory textureFactory;

    OpenGLContext(long handle)
    {
        super(handle);
        this.state = new OpenGLContextState(this);
        this.textureFactory = new OpenGLTextureFactory(this);
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
    public Framebuffer<OpenGLContext> getDefaultFramebuffer()
    {
        return new OpenGLDefaultFramebuffer(this);
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

    static int getPixelDataFormatFromDimensions(int dimensions)
    {
        switch(dimensions)
        {
        case 1: return GL_RED;
        case 2: return GL_RG;
        case 3: return GL_RGB;
        case 4: return GL_RGBA;
        default: throw new IllegalArgumentException("Data must be a vertex list of no more than 4 dimensions.");
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
        default: throw new IllegalArgumentException("Unrecognized data type."); // Shouldn't ever happen
        }
    }

    static void unbindBuffer(int bufferTarget, int index)
    {
        glBindBufferBase(bufferTarget, index, 0);
        errorCheck();
    }

    void unbindTextureUnit(int textureUnitIndex)
    {
        if (textureUnitIndex < 0)
        {
            throw new IllegalArgumentException("Texture unit index cannot be negative.");
        }
        else if (textureUnitIndex > state.getMaxCombinedTextureImageUnits())
        {
            throw new IllegalArgumentException("Texture unit index (" + textureUnitIndex + ") is greater than the maximum allowed index (" +
                    (state.getMaxCombinedTextureImageUnits()-1) + ").");
        }
        glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
        errorCheck();
        glBindTexture(GL_TEXTURE_1D, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_2D, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_3D, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_1D_ARRAY, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_RECTANGLE, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_BUFFER, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_CUBE_MAP_ARRAY, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);
        errorCheck();
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE_ARRAY, 0);
        errorCheck();
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
