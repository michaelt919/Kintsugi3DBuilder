package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import java.util.Map;
import java.util.TreeMap;

import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.DoubleVector2;
import tetzlaff.gl.helpers.DoubleVector3;
import tetzlaff.gl.helpers.DoubleVector4;
import tetzlaff.gl.helpers.IntVector2;
import tetzlaff.gl.helpers.IntVector3;
import tetzlaff.gl.helpers.IntVector4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;

class OpenGLRenderable implements Renderable<OpenGLContext>
{
	private static interface VertexAttributeSetting 
	{
		public void set();
	}
	
	protected final OpenGLContext context;
	
	private OpenGLProgram program;
	private OpenGLVertexArray vao;
	private Map<Integer, VertexAttributeSetting> settings;

	OpenGLRenderable(OpenGLContext context, OpenGLProgram program) 
	{
		this.context = context;
		this.program = program;
		this.vao = new OpenGLVertexArray(context);
		this.settings = new TreeMap<Integer, VertexAttributeSetting>();
	}
	
	@Override
	public OpenGLContext getContext()
	{
		return this.context;
	}
	
	@Override
	public void finalize()
	{
		vao.delete();
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
		case POINTS: return GL_POINTS;
		case TRIANGLES: return GL_TRIANGLES;
		case TRIANGLES_ADJACENCY: return GL_TRIANGLES_ADJACENCY;
		case TRIANGLE_FAN: return GL_TRIANGLE_FAN;
		case TRIANGLE_STRIP: return GL_TRIANGLE_STRIP;
		case TRIANGLE_STRIP_ADJACENCY: return GL_TRIANGLE_STRIP_ADJACENCY;
		default: throw new UnsupportedOperationException("Unrecognized primitive mode: " + primitiveMode);
		}
	}
	
	@Override
	public void draw(PrimitiveMode primitiveMode, Framebuffer<OpenGLContext> framebuffer)
	{
		if (framebuffer instanceof OpenGLFramebuffer)
		{
			((OpenGLFramebuffer)framebuffer).bindForDraw();
			program.use();
			for (VertexAttributeSetting s : settings.values())
			{
				s.set();
			}
			vao.draw(getOpenGLPrimitiveModeConst(primitiveMode));
		}
		else
		{
			throw new IllegalArgumentException("'framebuffer' must be of type OpenGLFramebuffer.");
		}
	}

	@Override
	public void draw(PrimitiveMode primitiveMode, Framebuffer<OpenGLContext> framebuffer, int x, int y, int width, int height)
	{
		if (framebuffer instanceof OpenGLFramebuffer)
		{
			((OpenGLFramebuffer)framebuffer).bindForDraw(x, y, width, height);
			program.use();
			for (VertexAttributeSetting s : settings.values())
			{
				s.set();
			}
			vao.draw(getOpenGLPrimitiveModeConst(primitiveMode));
		}
		else
		{
			throw new IllegalArgumentException("'framebuffer' must be of type OpenGLFramebuffer.");
		}
	}
	
	@Override
	public void draw(PrimitiveMode primitiveMode)
	{
		this.draw(primitiveMode, context.getDefaultFramebuffer());
	}

	@Override
	public void draw(PrimitiveMode primitiveMode, int x, int y, int width, int height)
	{
		this.draw(primitiveMode, context.getDefaultFramebuffer(), x, y, width, height);
	}
	
	@Override
	public boolean addVertexBuffer(int location, VertexBuffer<OpenGLContext> buffer)
	{
		if (buffer instanceof OpenGLVertexBuffer)
		{
			if (location >= 0)
			{
				this.vao.addVertexBuffer(location, buffer);
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			throw new IllegalArgumentException("'buffer' must be of type OpenGLVertexBuffer.");
		}
	}
	
	@Override
	public boolean addVertexBuffer(String name, VertexBuffer<OpenGLContext> buffer)
	{
		return this.addVertexBuffer(program.getVertexAttribLocation(name), buffer);
	}
	
	@Override
	public boolean setVertexAttrib(int location, int value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttribI1i(location, value);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, IntVector2 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttribI2i(location, value.x, value.y);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, IntVector3 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttribI3i(location, value.x, value.y, value.z);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, IntVector4 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttribI4i(location, value.x, value.y, value.z, value.w);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean setVertexAttrib(int location, float value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib1f(location, value);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, Vector2 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib2f(location, value.x, value.y);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, Vector3 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib3f(location, value.x, value.y, value.z);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, Vector4 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib4f(location, value.x, value.y, value.z, value.w);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean setVertexAttrib(int location, double value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib1d(location, value);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, DoubleVector2 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib2d(location, value.x, value.y);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, DoubleVector3 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib3d(location, value.x, value.y, value.z);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(int location, DoubleVector4 value)
	{
		if (location >= 0)
		{
			settings.put(location, () ->
			{
				glVertexAttrib4d(location, value.x, value.y, value.z, value.w);
				this.context.openGLErrorCheck();
			});
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setVertexAttrib(String name, int value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, IntVector2 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, IntVector3 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, IntVector4 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}

	@Override
	public boolean setVertexAttrib(String name, float value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, Vector2 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, Vector3 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, Vector4 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}

	@Override
	public boolean setVertexAttrib(String name, double value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, DoubleVector2 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, DoubleVector3 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
	
	@Override
	public boolean setVertexAttrib(String name, DoubleVector4 value)
	{
		int location = program.getVertexAttribLocation(name);
		return this.setVertexAttrib(location, value);
	}
}
