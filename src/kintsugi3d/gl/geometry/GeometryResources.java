/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.geometry;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.*;

public class GeometryResources<ContextType extends Context<ContextType>> implements Resource
{
    private static final Logger log = LoggerFactory.getLogger(GeometryResources.class);
    public final ContextType context;

    /**
     * The geometry for this instance that the vertex buffers were loaded from.
     */
    public final ReadonlyVertexGeometry geometry;

    /**
     * A vertex buffer containing vertex positions.
     */
    public final VertexBuffer<ContextType> positionBuffer;

    /**
     * A vertex buffer containing texture coordinates.
     */
    public final VertexBuffer<ContextType> texCoordBuffer;

    /**
     * A vertex buffer containing surface normals.
     */
    public final VertexBuffer<ContextType> normalBuffer;

    /**
     * A vertex buffer containing tangent vectors.
     */
    public final VertexBuffer<ContextType> tangentBuffer;

    /**
     * Default constructor: create null object
     */
    private GeometryResources()
    {
        this.context = null;
        this.geometry = null;
        this.positionBuffer = null;
        this.texCoordBuffer = null;
        this.normalBuffer = null;
        this.tangentBuffer = null;
    }

    public static <ContextType extends Context<ContextType>> GeometryResources<ContextType> createNullResources()
    {
        return new GeometryResources<>();
    }

    /**
     * package-visible constructor, called by VertexGeometry.createGraphicsResources
     */
    GeometryResources(ContextType context, ReadonlyVertexGeometry geometry)
    {
        this.context = context;
        this.geometry = geometry;
        this.positionBuffer = context.createVertexBuffer().setData(geometry.getVertices());

        if (geometry.hasTexCoords())
        {
            this.texCoordBuffer = context.createVertexBuffer().setData(geometry.getTexCoords());
        }
        else
        {
            this.texCoordBuffer = null;
        }

        if (geometry.hasNormals())
        {
            this.normalBuffer = context.createVertexBuffer().setData(geometry.getNormals());
        }
        else
        {
            this.normalBuffer = null;
        }

        if (geometry.hasTexCoords() && geometry.hasNormals())
        {
            this.tangentBuffer = context.createVertexBuffer().setData(geometry.getTangents());
        }
        else
        {
            this.tangentBuffer = null;
        }
    }

    /**
     * Creates a Drawable using this instance's geometry resources, and the specified shader program.
     * @param program The program to use to construct the Drawable.
     * @return A Drawable for rendering this instance using the specified shader program.
     */
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        Drawable<ContextType> drawable = program.getContext().createDrawable(program);
        drawable.addVertexBuffer("position", positionBuffer);
        drawable.addVertexBuffer("texCoord", texCoordBuffer);
        drawable.addVertexBuffer("normal", normalBuffer);
        drawable.addVertexBuffer("tangent", tangentBuffer);
        return drawable;
    }

    public GeometryFramebuffer<ContextType> createGeometryFramebuffer(int width, int height)
    {
        try
        {
            return new GeometryFramebuffer<>(this, width, height);
        }
        catch (IOException e)
        {
            log.error("File not found exception while trying to create a geometryFrameBuffer:", e);
            throw new UnsupportedOperationException(e);
        }
    }

    public boolean isNull()
    {
        return this.positionBuffer == null;
    }

    @Override
    public void close()
    {
        if (this.positionBuffer != null)
        {
            this.positionBuffer.close();
        }

        if (this.texCoordBuffer != null)
        {
            this.texCoordBuffer.close();
        }

        if (this.normalBuffer != null)
        {
            this.normalBuffer.close();
        }

        if (this.tangentBuffer != null)
        {
            this.tangentBuffer.close();
        }
    }
}
