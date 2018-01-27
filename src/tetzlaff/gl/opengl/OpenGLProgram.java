package tetzlaff.gl.opengl;

import java.util.*;

import tetzlaff.gl.builders.base.ProgramBuilderBase;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.Shader;
import tetzlaff.gl.core.Texture;
import tetzlaff.gl.core.UniformBuffer;
import tetzlaff.gl.exceptions.InvalidProgramException;
import tetzlaff.gl.exceptions.ProgramLinkFailureException;
import tetzlaff.gl.exceptions.UnlinkedProgramException;
import tetzlaff.gl.vecmath.*;
import tetzlaff.util.ResourceManager;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;

final class OpenGLProgram implements Program<OpenGLContext>
{
    final OpenGLContext context;

    private int programId;
    private AbstractCollection<OpenGLShader> ownedShaders;
    private ResourceManager<OpenGLTexture> textureManager;
    private ResourceManager<OpenGLUniformBuffer> uniformBufferManager;

    static class OpenGLProgramBuilder extends ProgramBuilderBase<OpenGLContext>
    {
        OpenGLProgramBuilder(OpenGLContext context)
        {
            super(context);
        }

        @Override
        public OpenGLProgram createProgram()
        {
            OpenGLProgram program = new OpenGLProgram(this.context);
            for(Shader<OpenGLContext> shader : this.getShaders())
            {
                program.attachShader(shader);
            }
            program.link();
            return program;
        }
    }

    private OpenGLProgram(OpenGLContext context)
    {
        this.context = context;
        initProgram();
    }

    private void initProgram()
    {
        programId = glCreateProgram();
        OpenGLContext.errorCheck();
        ownedShaders = new ArrayList<>(2);
        // Use one less texture unit because we don't use texture unit 0.
        textureManager = new ResourceManager<>(this.context.getState().getMaxCombinedTextureImageUnits() - 1);
        uniformBufferManager = new ResourceManager<>(this.context.getState().getMaxCombinedUniformBlocks());
    }

    private OpenGLProgram attachShader(Shader<OpenGLContext> shader)
    {
        if (shader instanceof OpenGLShader)
        {
            OpenGLShader shaderCast = (OpenGLShader)shader;
            glAttachShader(programId, shaderCast.getId());
            OpenGLContext.errorCheck();
            ownedShaders.add(shaderCast);
            return this;
        }
        else
        {
            throw new IllegalArgumentException("'shader' must be of type OpenGLShader.");
        }
    }

    private boolean isLinked()
    {
        int linked = glGetProgrami(programId, GL_LINK_STATUS);
        OpenGLContext.errorCheck();
        return linked == GL_TRUE;
    }

    private void link()
    {
        glLinkProgram(programId);
        OpenGLContext.errorCheck();
        if (!this.isLinked())
        {
            throw new ProgramLinkFailureException(glGetProgramInfoLog(programId));
        }
    }

    @Override
    public OpenGLContext getContext()
    {
        return this.context;
    }

    private void useForUniformAssignment()
    {
        if (this.isLinked())
        {
            glUseProgram(programId);
            OpenGLContext.errorCheck();
        }
        else
        {
            throw new UnlinkedProgramException("An OpenGL program cannot be used if it has not been linked.");
        }
    }

    private static final class TextureListWrapper extends AbstractList<Optional<OpenGLTexture>>
    {
        private final List<Optional<OpenGLTexture>> baseTextureList;

        private TextureListWrapper(List<Optional<OpenGLTexture>> baseTextureList)
        {
            this.baseTextureList = baseTextureList;
        }

        @Override
        public int size()
        {
            return baseTextureList.size() + 1;
        }

        @Override
        public Optional<OpenGLTexture> get(int index)
        {
            return index == 0 ? Optional.empty() : baseTextureList.get(index - 1);
        }
    }

    void use()
    {
        if (this.isLinked())
        {
            this.context.updateTextureBindings(new TextureListWrapper(textureManager.asReadonlyList()));
            this.context.updateUniformBufferBindings(uniformBufferManager.asReadonlyList());

            glValidateProgram(programId);
            OpenGLContext.errorCheck();
            if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE)
            {
                OpenGLContext.errorCheck();

                String programInfoLog = glGetProgramInfoLog(programId);
                OpenGLContext.errorCheck();

                throw new InvalidProgramException(programInfoLog);
            }
            else
            {
                OpenGLContext.errorCheck();
            }

            glUseProgram(programId);
            OpenGLContext.errorCheck();
        }
        else
        {
            throw new UnlinkedProgramException("An OpenGL program cannot be used if it has not been linked.");
        }
    }

    @Override
    public boolean setTexture(int location, Texture<OpenGLContext> texture)
    {
        if (texture == null)
        {
            return this.setUniform(location, 0);
        }
        else if (texture instanceof OpenGLTexture)
        {
            // We don't use texture unit 0, so add one to the resource ID.
            int textureUnit = 1 + textureManager.assignResourceByKey(location, (OpenGLTexture)texture);
            return this.setUniform(location, textureUnit);
        }
        else
        {
            throw new IllegalArgumentException("'texture' must be of type OpenGLTexture.");
        }
    }

    @Override
    public boolean setTexture(String name, Texture<OpenGLContext> texture)
    {
        return this.setTexture(this.getUniformLocation(name), texture);
    }

    @Override
    public boolean setUniformBuffer(int index, UniformBuffer<OpenGLContext> buffer)
    {
        if (buffer instanceof OpenGLUniformBuffer)
        {
            if (index >= 0)
            {
                int bindingPoint = uniformBufferManager.assignResourceByKey(index, (OpenGLUniformBuffer)buffer);

                glUniformBlockBinding(this.programId, index, bindingPoint);
                OpenGLContext.errorCheck();

                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            throw new IllegalArgumentException("'buffer' must be of type OpenGLUniformBuffer.");
        }
    }

    @Override
    public boolean setUniformBuffer(String name, UniformBuffer<OpenGLContext> buffer)
    {
        return this.setUniformBuffer(this.getUniformBlockIndex(name), buffer);
    }

    @Override
    public int getUniformBlockIndex(String name)
    {
        int index = glGetUniformBlockIndex(this.programId, name);
        OpenGLContext.errorCheck();
        return index;
    }

    @Override
    public void close()
    {
        glDeleteProgram(programId);
        OpenGLContext.errorCheck();
        for (OpenGLShader shader : ownedShaders)
        {
            shader.close();
        }
    }

    @Override
    public int getUniformLocation(String name)
    {
        int location = glGetUniformLocation(programId, name);
        OpenGLContext.errorCheck();
        return location;
    }

    @Override
    public boolean setUniform(int location, int value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform1i(location, value);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, IntVector2 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform2i(location, value.x, value.y);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, IntVector3 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform3i(location, value.x, value.y, value.z);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, IntVector4 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform4i(location, value.x, value.y, value.z, value.w);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, float value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform1f(location, value);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, Vector2 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform2f(location, value.x, value.y);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, Vector3 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform3f(location, value.x, value.y, value.z);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, Vector4 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform4f(location, value.x, value.y, value.z, value.w);
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(int location, boolean value)
    {
        return this.setUniform(location, value ? GL_TRUE : GL_FALSE);
    }

    @Override
    public boolean setUniform(String name, boolean value)
    {
        return this.setUniform(this.getUniformLocation(name), value ? GL_TRUE : GL_FALSE);
    }

    @Override
    public boolean setUniform(String name, int value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public boolean setUniform(String name, IntVector2 value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public boolean setUniform(String name, IntVector3 value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public boolean setUniform(String name, IntVector4 value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public boolean setUniform(String name, float value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public boolean setUniform(String name, Vector2 value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public boolean setUniform(String name, Vector3 value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public boolean setUniform(String name, Vector4 value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }

    @Override
    public int getVertexAttribLocation(String name)
    {
        int location = glGetAttribLocation(programId, name);
        OpenGLContext.errorCheck();
        return location;
    }

    @Override
    public boolean setUniform(int location, Matrix4 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniformMatrix4fv(location, false, value.asFloatBuffer());
            OpenGLContext.errorCheck();

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setUniform(String name, Matrix4 value)
    {
        return this.setUniform(this.getUniformLocation(name), value);
    }
}
