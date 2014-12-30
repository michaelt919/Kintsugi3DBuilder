package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.openGLErrorCheck;

import java.util.Map;
import java.util.TreeMap;

import tetzlaff.gl.Context;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.exceptions.UnrecognizedPrimitiveModeException;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.helpers.VertexAttributeSetting;

public class OpenGLRenderable implements Renderable<OpenGLProgram, OpenGLVertexBuffer, OpenGLFramebuffer, OpenGLTexture>
{
	private OpenGLProgram program;
	private OpenGLVertexArray vao;
	private boolean vaoOwned;
	private Map<Integer, VertexAttributeSetting> settings;

	public OpenGLRenderable(OpenGLProgram program, OpenGLVertexArray vertexArray, boolean ownVertexArray) 
	{
		this.program = program;
		this.vao = vertexArray;
		this.vaoOwned = ownVertexArray;
		this.settings = new TreeMap<Integer, VertexAttributeSetting>();
	}

	public OpenGLRenderable(OpenGLProgram program, OpenGLVertexArray vertexArray) 
	{
		this(program, vertexArray, false);
	}
	
	public OpenGLRenderable(OpenGLProgram program) 
	{
		this(program, new OpenGLVertexArray(), true);
	}

	@Override
	public void finalize()
	{
		if (vaoOwned)
		{
			vao.delete();
		}
	}
	
	@Override
	public OpenGLProgram program()
	{
		return this.program;
	}
	
	private int getOpenGLPrimitiveModeConst(PrimitiveMode primitiveMode)
	{
		switch(primitiveMode)
		{
		case LINES: return GL_LINES;
		case LINES_ADJACENCY: return GL_LINES_ADJACENCY;
		case LINE_LOOP: return GL_LINE_LOOP;
		case LINE_STRIP: return GL_LINE_STRIP;
		case LINE_STRIP_ADJACENCY: return GL_LINE_STRIP_ADJACENCY;
		case PATCHES: return GL_PATCHES;
		case POINTS: return GL_POINTS;
		case TRIANGLES: return GL_TRIANGLES;
		case TRIANGLES_ADJACENCY: return GL_TRIANGLES_ADJACENCY;
		case TRIANGLE_FAN: return GL_TRIANGLE_FAN;
		case TRIANGLE_STRIP: return GL_TRIANGLE_STRIP;
		case TRIANGLE_STRIP_ADJACENCY: return GL_TRIANGLE_STRIP_ADJACENCY;
		default: throw new UnrecognizedPrimitiveModeException("Unrecognized primitive mode: " + primitiveMode);
		}
	}
	
	@Override
	public void draw(PrimitiveMode primitiveMode, OpenGLFramebuffer framebuffer)
	{
		framebuffer.bindForDraw();
		program.use();
		for (VertexAttributeSetting s : settings.values())
		{
			s.set();
		}
		vao.draw(getOpenGLPrimitiveModeConst(primitiveMode));
	}

	@Override
	public void draw(PrimitiveMode primitiveMode, OpenGLFramebuffer framebuffer, int x, int y, int width, int height)
	{
		framebuffer.bindForDraw(x, y, width, height);
		program.use();
		for (VertexAttributeSetting s : settings.values())
		{
			s.set();
		}
		vao.draw(getOpenGLPrimitiveModeConst(primitiveMode));
	}

	@Override
	public void draw(PrimitiveMode primitiveMode, OpenGLFramebuffer framebuffer, int width, int height)
	{
		this.draw(primitiveMode, framebuffer, 0, 0, width, height);
	}
	
	@Override
	public void draw(PrimitiveMode primitiveMode, Context context)
	{
		this.draw(primitiveMode, OpenGLDefaultFramebuffer.fromContext(context));
	}

	@Override
	public void draw(PrimitiveMode primitiveMode, Context context, int width, int height)
	{
		this.draw(primitiveMode, OpenGLDefaultFramebuffer.fromContext(context), width, height);
	}

	@Override
	public void draw(PrimitiveMode primitiveMode, Context context, int x, int y, int width, int height)
	{
		this.draw(primitiveMode, OpenGLDefaultFramebuffer.fromContext(context), x, y, width, height);
	}
	
	@Override
	public void addVertexBuffer(int location, OpenGLVertexBuffer buffer, boolean owned)
	{
		this.vao.addVertexBuffer(location, buffer, owned);
	}
	
	@Override
	public void addVertexBuffer(String name, OpenGLVertexBuffer buffer, boolean owned)
	{
		this.addVertexBuffer(program.getVertexAttribLocation(name), buffer, owned);
	}
	
	@Override
	public void addVertexBuffer(int location, OpenGLVertexBuffer buffer)
	{
		this.vao.addVertexBuffer(location, buffer);
	}
	
	@Override
	public void addVertexBuffer(String name, OpenGLVertexBuffer buffer)
	{
		this.addVertexBuffer(program.getVertexAttribLocation(name), buffer);
	}

	@Override
	public void addVertexMesh(String vertexName, String texCoordName, String normalName, VertexMesh mesh) 
	{
        this.addVertexBuffer(
    		vertexName,
    		new OpenGLVertexBuffer(mesh.getVertices()),
			true
        );
        if (mesh.hasTexCoords())
        {
	        this.addVertexBuffer(
	    		texCoordName, 
	    		new OpenGLVertexBuffer(mesh.getTexCoords()),
				true
	        );
        }
        if (mesh.hasNormals())
        {
	        this.addVertexBuffer(
	    		normalName, 
	    		new OpenGLVertexBuffer(mesh.getNormals()),
				true
	        );
        }
	}
	
	@Override
	public void setVertexAttrib(int location, int value)
	{
		settings.put(location, () ->
		{
			glVertexAttribI1i(location, value);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, int value1, int value2)
	{
		settings.put(location, () ->
		{
			glVertexAttribI2i(location, value1, value2);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, int value1, int value2, int value3)
	{
		settings.put(location, () ->
		{
			glVertexAttribI3i(location, value1, value2, value3);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, int value1, int value2, int value3, int value4)
	{
		settings.put(location, () ->
		{
			glVertexAttribI4i(location, value1, value2, value3, value4);
			openGLErrorCheck();
		});
	}

	@Override
	public void setVertexAttrib(int location, float value)
	{
		settings.put(location, () ->
		{
			glVertexAttrib1f(location, value);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, float value1, float value2)
	{
		settings.put(location, () ->
		{
			glVertexAttrib2f(location, value1, value2);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, float value1, float value2, float value3)
	{
		settings.put(location, () ->
		{
			glVertexAttrib3f(location, value1, value2, value3);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, float value1, float value2, float value3, float value4)
	{
		settings.put(location, () ->
		{
			glVertexAttrib4f(location, value1, value2, value3, value4);
			openGLErrorCheck();
		});
	}

	@Override
	public void setVertexAttrib(int location, double value)
	{
		settings.put(location, () ->
		{
			glVertexAttrib1d(location, value);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, double value1, double value2)
	{
		settings.put(location, () ->
		{
			glVertexAttrib2d(location, value1, value2);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, double value1, double value2, double value3)
	{
		settings.put(location, () ->
		{
			glVertexAttrib3d(location, value1, value2, value3);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(int location, double value1, double value2, double value3, double value4)
	{
		settings.put(location, () ->
		{
			glVertexAttrib4d(location, value1, value2, value3, value4);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, int value)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttribI1i(location, value);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, int value1, int value2)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttribI2i(location, value1, value2);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, int value1, int value2, int value3)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttribI3i(location, value1, value2, value3);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, int value1, int value2, int value3, int value4)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttribI4i(location, value1, value2, value3, value4);
			openGLErrorCheck();
		});
	}

	@Override
	public void setVertexAttrib(String name, float value)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib1f(location, value);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, float value1, float value2)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib2f(location, value1, value2);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, float value1, float value2, float value3)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib3f(location, value1, value2, value3);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, float value1, float value2, float value3, float value4)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib4f(location, value1, value2, value3, value4);
			openGLErrorCheck();
		});
	}

	@Override
	public void setVertexAttrib(String name, double value)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib1d(location, value);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, double value1, double value2)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib2d(location, value1, value2);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, double value1, double value2, double value3)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib3d(location, value1, value2, value3);
			openGLErrorCheck();
		});
	}
	
	@Override
	public void setVertexAttrib(String name, double value1, double value2, double value3, double value4)
	{
		int location = program.getVertexAttribLocation(name);
		settings.put(location, () ->
		{
			glVertexAttrib4d(location, value1, value2, value3, value4);
			openGLErrorCheck();
		});
	}
}
