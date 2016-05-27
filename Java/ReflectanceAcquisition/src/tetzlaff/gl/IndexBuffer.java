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
 * An interface for a buffer object that can serve as an "index buffer" or "element array buffer" in conjunction with one or more vertex buffer objects (VBOs).
 * This buffer specifies the order in which vertices in VBOs should be processed during a draw call.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface IndexBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
	/**
	 * Gets the number of indices in the buffer.
	 * @return The number of indices in the buffer.
	 */
	int count();
	
	/**
	 * Sets the content of the index buffer.
	 * @param data An array containing the array indices.
	 * @return The calling object.
	 */
	IndexBuffer<ContextType> setData(int[] data);
}
