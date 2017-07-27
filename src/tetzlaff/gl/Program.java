package tetzlaff.gl;

import tetzlaff.gl.vecmath.IntVector2;
import tetzlaff.gl.vecmath.IntVector3;
import tetzlaff.gl.vecmath.IntVector4;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;

/**
 * An interface for a program that can be used for rendering.
 * A program's behavior can be modified dynamically by changing "uniform" variables which remain constant with respect to a single draw call.
 * These variables can be scalars, vectors, uniform buffer objects (which consist of multiple scalars or vectors stored in a single buffer), or texture objects.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the program is associated with.
 */
public interface Program<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
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