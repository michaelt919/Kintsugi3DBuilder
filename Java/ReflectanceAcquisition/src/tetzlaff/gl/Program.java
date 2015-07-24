package tetzlaff.gl;

import tetzlaff.gl.helpers.IntVector2;
import tetzlaff.gl.helpers.IntVector3;
import tetzlaff.gl.helpers.IntVector4;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;

public interface Program<ContextType extends Context<? super ContextType>> extends Resource
{

	void attachShader(Shader<ContextType> shader, boolean owned);

	void detachShader(Shader<ContextType> shader);

	boolean isLinked();

	void link();

	boolean setUniform(String name, boolean value);

	boolean setUniform(String name, Vector4 value);

	boolean setUniform(String name, Vector3 value);

	boolean setUniform(String name, Vector2 value);

	boolean setUniform(String name, float value);

	boolean setUniform(String name, IntVector4 value);

	boolean setUniform(String name, IntVector3 value);

	boolean setUniform(String name, IntVector2 value);

	boolean setUniform(String name, int value);
	
	boolean setUniform(String name, Matrix4 value);

	boolean setUniform(int location, boolean value);
	
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

	boolean setTexture(int location, Texture<ContextType> texture);

	boolean setTexture(String name, Texture<ContextType> texture);

	boolean setUniformBuffer(int index, UniformBuffer<ContextType> buffer);

	boolean setUniformBuffer(String name, UniformBuffer<ContextType> buffer);

	int getUniformBlockIndex(String name);

}