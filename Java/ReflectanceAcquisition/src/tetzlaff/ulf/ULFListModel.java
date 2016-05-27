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
package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import javax.swing.ComboBoxModel;

import tetzlaff.gl.Context;

/**
 * An interface for a list of unstructured light fields that also serves as the model for a combo box where a light field can be selected.
 * @author Michael Tetzlaff
 *
 */
public interface ULFListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<ULFDrawable<ContextType>>
{
	/**
	 * Adds a new unstructured light field to the model from a view set file.
	 * @param vsetFile The view set file defining the light field to be added.
	 * @param loadOptions The options to use when loading the light field.
	 * @return The newly added unstructured light field as a ULFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	ULFDrawable<ContextType> addFromVSETFile(File vsetFile, ULFLoadOptions loadOptions) throws IOException;
	
	/**
	 * Adds a new unstructured light field to the model from Agisoft PhotoScan.
	 * @param xmlFile The Agisoft PhotoScan XML camera file defining the views of the light field to be added.
     * @param meshFile The mesh exported from Agisoft PhotoScan to be used as proxy geometry.
	 * @param loadOptions The options to use when loading the light field.
	 * @return The newly added unstructured light field as a ULFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	ULFDrawable<ContextType> addFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException;
	
	/**
	 * Adds a new unstructured light field morph to the model.
	 * @param lfmFile The light field morph file defining the morph to be added.
	 * @param loadOptions The options to use when loading the light field.
	 * @return The newly added unstructured light field morph as a ULFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	ULFDrawable<ContextType> addMorphFromLFMFile(File lfmFile, ULFLoadOptions loadOptions) throws IOException;
	
	/**
	 * Sets a loading monitor with callbacks that are fired when the light field finishes loading and/or at certain checkpoints when loading.
	 * @param loading monitor The loading monitor.
	 */
	void setLoadingMonitor(ULFLoadingMonitor loadingMonitor);
	
	@Override
	ULFDrawable<ContextType> getSelectedItem();
}
