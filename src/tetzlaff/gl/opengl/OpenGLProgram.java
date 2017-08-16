package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;

import java.util.AbstractCollection;
import java.util.ArrayList;

import tetzlaff.gl.Program;
import tetzlaff.gl.Resource;
import tetzlaff.gl.Shader;
import tetzlaff.gl.Texture;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.builders.base.ProgramBuilderBase;
import tetzlaff.gl.exceptions.InvalidProgramException;
import tetzlaff.gl.exceptions.ProgramLinkFailureException;
import tetzlaff.gl.exceptions.UnlinkedProgramException;
import tetzlaff.gl.vecmath.IntVector2;
import tetzlaff.gl.vecmath.IntVector3;
import tetzlaff.gl.vecmath.IntVector4;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.util.ResourceManager;

class OpenGLProgram implements Resource, Program<OpenGLContext>
{
    protected final OpenGLContext context;

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
        this.context.openGLErrorCheck();
        ownedShaders = new ArrayList<OpenGLShader>();
        // Use one less texture unit because we don't use texture unit 0.
        textureManager = new ResourceManager<OpenGLTexture>(this.context.getState().getMaxCombinedTextureImageUnits() - 1);
        uniformBufferManager = new ResourceManager<OpenGLUniformBuffer>(this.context.getState().getMaxCombinedUniformBlocks());
    }

    private OpenGLProgram attachShader(Shader<OpenGLContext> shader)
    {
        if (shader instanceof OpenGLShader)
        {
            OpenGLShader shaderCast = (OpenGLShader)shader;
            glAttachShader(programId, shaderCast.getId());
            this.context.openGLErrorCheck();
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
        this.context.openGLErrorCheck();
        return linked == GL_TRUE;
    }

    private void link()
    {
        glLinkProgram(programId);
        this.context.openGLErrorCheck();
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
        if (!this.isLinked())
        {
            throw new UnlinkedProgramException("An OpenGL program cannot be used if it has not been linked.");
        }
        else
        {
            glUseProgram(programId);
            this.context.openGLErrorCheck();
        }
    }

    void use()
    {
        if (!this.isLinked())
        {
            throw new UnlinkedProgramException("An OpenGL program cannot be used if it has not been linked.");
        }
        else
        {
            this.context.unbindTextureUnit(0); // Don't ever use texture unit 0

            for (int i = 0; i < textureManager.length; i++)
            {
                OpenGLTexture texture = textureManager.getResourceByUnit(i);
                this.context.unbindTextureUnit(i + 1);
                if (texture != null)
                {
                    texture.bindToTextureUnit(i + 1);
                }
            }

            for (int i = 0; i < uniformBufferManager.length; i++)
            {
                OpenGLUniformBuffer uniformBuffer = uniformBufferManager.getResourceByUnit(i);
                if (uniformBuffer != null)
                {
                    uniformBuffer.bindToIndex(i);
                }
                else
                {
                    this.context.unbindBuffer(GL_UNIFORM_BUFFER, i);
                }
            }

            glValidateProgram(programId);
            if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE)
            {
                throw new InvalidProgramException("The OpenGL program to be used failed validation.");
            }

            glUseProgram(programId);
            this.context.openGLErrorCheck();
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
                this.context.openGLErrorCheck();

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
        this.context.openGLErrorCheck();
        return index;
    }

    @Override
    public void close()
    {
        glDeleteProgram(programId);
        this.context.openGLErrorCheck();
        for (OpenGLShader shader : ownedShaders)
        {
            shader.close();
        }
    }

    @Override
    public int getUniformLocation(String name)
    {
        int location = glGetUniformLocation(programId, name);
        this.context.openGLErrorCheck();
        return location;
    }

    @Override
    public boolean setUniform(int location, int value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniform1i(location, value);
            this.context.openGLErrorCheck();

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
            this.context.openGLErrorCheck();

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
            this.context.openGLErrorCheck();

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
            this.context.openGLErrorCheck();

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
            this.context.openGLErrorCheck();

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
            this.context.openGLErrorCheck();

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
            this.context.openGLErrorCheck();

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
            this.context.openGLErrorCheck();

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
        this.context.openGLErrorCheck();
        return location;
    }

    @Override
    public boolean setUniform(int location, Matrix4 value)
    {
        if (location >= 0)
        {
            this.useForUniformAssignment();

            glUniformMatrix4fv(location, false, value.asFloatBuffer());
            this.context.openGLErrorCheck();

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
