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
package tetzlaff.helpers;

/**
 * An interface for a collection of items of which one can be selected.
 * @author Michael Tetzlaff
 *
 * @param <T> The type of objects in this collection.
 */
public interface Selectable<T>
{
	/**
	 * Gets the index of the currently selected item.
	 * @return The index of the selected item.
	 */
	int getSelectedIndex();
	
	/**
	 * Gets the currently selected item.
	 * @return The selected item.
	 */
	T getSelectedItem();
	
	/**
	 * Sets the index of the currently selected item.
	 * @param index The index of the selected item.
	 */
	void setSelectedIndex(int index);
	
	/**
	 * Sets which item should be currently selected.
	 * If this exact item is not in the collection, this method has no effect.
	 * @param item The item to be selected.
	 */
	void setSelectedItem(Object item);
}
