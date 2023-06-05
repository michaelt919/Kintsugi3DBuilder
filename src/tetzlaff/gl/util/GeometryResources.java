/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.util;

import tetzlaff.gl.core.*;

public class GeometryResources<ContextType extends Context<ContextType>> implements Resource
{
    /**
     * The geometry for this instance that the vertex buffers were loaded from.
     */
    public final VertexGeometry geometry;

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
    GeometryResources(ContextType context, VertexGeometry geometry)
    {
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
