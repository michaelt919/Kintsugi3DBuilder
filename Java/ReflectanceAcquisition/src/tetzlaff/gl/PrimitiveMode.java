/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl;

/**
 * Enumerates the possible primitive modes which can be used for a drawing operation.
 * @author Michael Tetzlaff
 *
 */
public enum PrimitiveMode 
{
	/**
	 * Vertices are drawn as individual unconnected points on the screen.
	 */
	POINTS, 
	
	/**
	 * Vertices are interpreted as a continuous sequence of points, drawn on the screen as connected line segments.
	 */
	LINE_STRIP, 
	
	/**
	 * Vertices are interpreted as a continuous sequence of points, drawn on the screen as connected line segments, 
	 * with a final line segment connecting the first and last vertex to form a loop.
	 */
	LINE_LOOP, 
	
	/**
	 * Pairs of vertices are interpreted as individual line segments to be drawn on the screen.
	 */
	LINES, 
	
	/**
	 * Vertices are interpreted as a continuous sequence of points, drawn on the screen as connected line segments.
	 * Adjacent line segments are available to geometry shaders.
	 */
	LINE_STRIP_ADJACENCY, 
	
	/**
	 * Pairs of vertices are interpreted as individual line segments to be drawn on the screen.
	 * Adjacent line segments are available to geometry shaders.
	 */
	LINES_ADJACENCY, 
	
	/**
	 * Vertices are interpreted as a continuous sequence of points, where each subsequence of three vertices is drawn on the screen as a triangle.
	 */
	TRIANGLE_STRIP, 
	
	/**
	 * Vertices are interpreted as an initial pivot vertex and then a continuous sequence of points, 
	 * where any two adjacent vertices are drawn as a triangle with the pivot vertex.
	 */
	TRIANGLE_FAN, 
	
	/**
	 * Each group of three vertices is interpreted as an individual triangle to be drawn on the screen.
	 */
	TRIANGLES, 
	
	/**
	 * Vertices are interpreted as a continuous sequence of points, where each subsequence of three vertices is drawn on the screen as a triangle.
	 * Adjacent triangles are available to geometry shaders.
	 */
	TRIANGLE_STRIP_ADJACENCY, 
	
	/**
	 * Each group of three vertices is interpreted as an individual triangle to be drawn on the screen.
	 * Adjacent triangles are available to geometry shaders.
	 */
	TRIANGLES_ADJACENCY, 
	
	/**
	 * Groups of vertices are interpreted as arbitrary primitives to be tessellated into points, lines, and triangles.
	 * Can only be used if tessellation shaders are present.
	 */
	PATCHES
}
