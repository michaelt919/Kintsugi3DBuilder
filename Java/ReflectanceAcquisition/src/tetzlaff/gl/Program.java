package tetzlaff.gl;

import tetzlaff.gl.helpers.*;
import tetzlaff.gl.opengl.OpenGLUniformBuffer;

public interface Program<ShaderType, TextureType, UniformBufferType>
{

	void attachShader(ShaderType shader, boolean owned);

	void detachShader(ShaderType shader);

	boolean isLinked();

	void link();

	boolean setUniform(String name, Vector4 value);

	boolean setUniform(String name, Vector3 value);

	boolean setUniform(String name, Vector2 value);

	boolean setUniform(String name, float value);

	boolean setUniform(String name, IntVector4 value);

	boolean setUniform(String name, IntVector3 value);

	boolean setUniform(String name, IntVector2 value);

	boolean setUniform(String name, int value);
	
	boolean setUniform(String name, Matrix4 value);

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

	boolean setTexture(int location, TextureType texture);

	boolean setTexture(String name, TextureType texture);

	boolean setUniformBuffer(int index, UniformBufferType buffer);

	boolean setUniformBuffer(String name, UniformBufferType buffer);

	int getUniformBlockIndex(String name);

}