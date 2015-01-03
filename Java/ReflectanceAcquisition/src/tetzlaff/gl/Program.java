package tetzlaff.gl;

import tetzlaff.gl.helpers.*;
import tetzlaff.gl.opengl.OpenGLUniformBuffer;

public interface Program<ShaderType, TextureType, UniformBufferType>
{

	void attachShader(ShaderType shader, boolean owned);

	void detachShader(ShaderType shader);

	boolean isLinked();

	void link();

	void setUniform(String name, DoubleVector4 value);

	void setUniform(String name, DoubleVector3 value);

	void setUniform(String name, DoubleVector2 value);

	void setUniform(String name, double value);

	void setUniform(String name, Vector4 value);

	void setUniform(String name, Vector3 value);

	void setUniform(String name, Vector2 value);

	void setUniform(String name, float value);

	void setUniform(String name, IntVector4 value);

	void setUniform(String name, IntVector3 value);

	void setUniform(String name, IntVector2 value);

	void setUniform(String name, int value);
	
	void setUniform(String name, Matrix4 value);

	void setUniform(int location, DoubleVector4 value);

	void setUniform(int location, DoubleVector3 value);

	void setUniform(int location, DoubleVector2 value);

	void setUniform(int location, double value);

	void setUniform(int location, Vector4 value);

	void setUniform(int location, Vector3 value);

	void setUniform(int location, Vector2 value);

	void setUniform(int location, float value);

	void setUniform(int location, IntVector4 value);

	void setUniform(int location, IntVector3 value);

	void setUniform(int location, IntVector2 value);

	void setUniform(int location, int value);
	
	void setUniform(int location, Matrix4 value);

	int getUniformLocation(String name);

	int getVertexAttribLocation(String name);

	void setTexture(int location, TextureType texture);

	void setTexture(String name, TextureType texture);

	void setUniformBuffer(int index, UniformBufferType buffer);

	void setUniformBuffer(String name, UniformBufferType buffer);

	int getUniformBlockIndex(String name);

}