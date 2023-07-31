/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.opengl;

import java.util.Map;
import java.util.TreeMap;

import kintsugi3d.gl.core.*;
import kintsugi3d.gl.exceptions.UnrecognizedPrimitiveModeException;
import kintsugi3d.gl.vecmath.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

class OpenGLDrawable extends DrawableBase<OpenGLContext> implements Drawable<OpenGLContext>
{
    private interface VertexAttributeSetting
    {
        void set();
    }

    protected final OpenGLContext context;

    private final OpenGLProgramObject program;
    private final OpenGLVertexArray vao;
    private final Map<Integer, VertexAttributeSetting> settings;

    OpenGLDrawable(OpenGLContext context, OpenGLProgramObject program)
    {
        this.context = context;
        this.program = program;
        this.vao = new OpenGLVertexArray(context);
        this.settings = new TreeMap<>();
    }

    @Override
    public OpenGLContext getContext()
    {
        return this.context;
    }

    @Override
    protected void finalize()
    {
        vao.close();
    }

    @Override
    public OpenGLProgramObject program()
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
        default: throw new UnrecognizedPrimitiveModeException("Unrecognized primitive mode: " + primitiveMode);
        }
    }

    @Override
    public void draw(PrimitiveMode primitiveMode, Framebuffer<OpenGLContext> framebuffer)
    {
        framebuffer.getDrawContents().bindForDraw();
        program.use();
        for (VertexAttributeSetting s : settings.values())
        {
            s.set();
        }
        vao.draw(getOpenGLPrimitiveModeConst(primitiveMode));
    }

    @Override
    public void draw(PrimitiveMode primitiveMode, Framebuffer<OpenGLContext> framebuffer, int x, int y, int width, int height)
    {
        framebuffer.getDrawContents().bindViewportForDraw(x, y, width, height);
        program.use();
        for (VertexAttributeSetting s : settings.values())
        {
            s.set();
        }
        vao.draw(getOpenGLPrimitiveModeConst(primitiveMode));
    }

    @Override
    public void draw(PrimitiveMode primitiveMode, Framebuffer<OpenGLContext> framebuffer, int width, int height)
    {
        this.draw(primitiveMode, framebuffer, 0, 0, width, height);
    }

    @Override
    public void draw(PrimitiveMode primitiveMode, OpenGLContext context)
    {
        this.draw(primitiveMode, context.getDefaultFramebuffer());
    }

    @Override
    public void draw(PrimitiveMode primitiveMode, OpenGLContext context, int width, int height)
    {
        this.draw(primitiveMode, context.getDefaultFramebuffer(), width, height);
    }

    @Override
    public void draw(PrimitiveMode primitiveMode, OpenGLContext context, int x, int y, int width, int height)
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
                OpenGLContext.errorCheck();
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
