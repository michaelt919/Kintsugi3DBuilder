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
package tetzlaff.lightfield;

import java.io.File;
import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.MultiDrawable;
import tetzlaff.gl.helpers.Trackball;

/**
 * An abstract class defining the skeleton of an implementation of a LFListModel as a list of LFDrawable entities.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that will be used for rendering.
 */
public abstract class LFDrawableListModel<ContextType extends Context<ContextType>> extends AbstractListModel<LFDrawable<ContextType>> implements LFListModel<ContextType>
{
	private static final long serialVersionUID = 4167467314632694946L;
	
	/**
	 * The context that will be used for rendering.
	 */
	protected final ContextType context;
	
	/**
	 * The program to use for rendering.
	 */
	private Program<ContextType> program;
	
	/**
	 * The trackball controlling the movement of the virtual camera.
	 */
	protected final Trackball trackball;
	
	
	private MultiDrawable<LFDrawable<ContextType>> lightfields;
	private int effectiveSize;
	private LFLoadingMonitor loadingMonitor;
	
	/**
	 * Creates a new LFDrawableListModel.
	 * @param context The context that will be used for rendering.
	 * @param trackball The trackball controlling the movement of the virtual camera.
	 */
	public LFDrawableListModel(ContextType context, Trackball trackball) 
	{
		this.context = context;
		this.trackball = trackball;
		this.lightfields = new MultiDrawable<LFDrawable<ContextType>>();
		this.effectiveSize = 0;
		
		try
        {
			this.program = context.getShaderProgramBuilder()
					.addShader(ShaderType.VERTEX, new File(LightField.SHADER_RESOURCE_DIRECTORY, "ulr.vert"))
					.addShader(ShaderType.FRAGMENT, new File(LightField.SHADER_RESOURCE_DIRECTORY, "ulr.frag"))
					.createProgram();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        	throw new IllegalStateException("The shader program could not be initialized.", e);
        }
	}
	
	public Program<ContextType> getProgram()
	{
		return this.program;
	}
	
	public void setProgram(Program<ContextType> program)
	{
		this.program = program;
		for (LFDrawable<ContextType> lf : lightfields)
		{
			lf.setProgram(program);
		}
	}
	
	/**
	 * Required in order to define how to load an light field from a view set file.
	 * @param vsetFile The view set file defining the light field to be added.
	 * @param loadOptions The options to use when loading the light field.
	 * @return A new light field as a LFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	protected abstract LFDrawable<ContextType> createFromVSETFile(File vsetFile, LFLoadOptions loadOptions) throws IOException;
	
	/**
	 * Required in order to define how to load a light field from Agisoft PhotoScan.
	 * @param xmlFile The Agisoft PhotoScan XML camera file defining the views of the light field to be added.
     * @param meshFile The mesh exported from Agisoft PhotoScan to be used as proxy geometry.
	 * @param loadOptions The options to use when loading the light field.
	 * @return A new light field as a LFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	protected abstract LFDrawable<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, LFLoadOptions loadOptions) throws IOException;
	
	/**
	 * Required in order to define how to load a light field morph.
	 * @param lfmFile The light field morph file defining the morph to be added.
	 * @param loadOptions The options to use when loading the light field.
	 * @return A new light field morph as a LFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	protected abstract LFDrawable<ContextType> createMorphFromLFMFile(File lfmFile, LFLoadOptions loadOptions) throws IOException;

	@Override
	public LFDrawable<ContextType> addFromVSETFile(File vsetFile, LFLoadOptions loadOptions) throws IOException
	{
		LFDrawable<ContextType> newItem = this.createFromVSETFile(vsetFile, loadOptions);
		newItem.setOnLoadCallback(new LFLoadingMonitor()
		{
			@Override
			public void startLoading()
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.startLoading();
				}
			}
			
			@Override
			public void setMaximum(double maximum)
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setMaximum(maximum);
				}
			}

			@Override
			public void setProgress(double progress) 
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setProgress(progress);
				}
			}
			
			@Override
			public void loadingComplete()
			{
				lightfields.setSelectedItem(newItem);
				effectiveSize = lightfields.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, lightfields.size() - 1, lightfields.size() - 1);
			}
		});
		lightfields.add(newItem);
		return newItem;
	}
	
	@Override
	public LFDrawable<ContextType> addFromAgisoftXMLFile(File xmlFile, File meshFile, LFLoadOptions loadOptions) throws IOException
	{
		LFDrawable<ContextType> newItem = this.createFromAgisoftXMLFile(xmlFile, meshFile, loadOptions);
		newItem.setOnLoadCallback(new LFLoadingMonitor()
		{
			@Override
			public void startLoading()
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.startLoading();
				}
			}
			
			@Override
			public void setMaximum(double maximum)
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setMaximum(maximum);
				}
			}

			@Override
			public void setProgress(double progress) 
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setProgress(progress);
				}
			}
			
			@Override
			public void loadingComplete()
			{
				lightfields.setSelectedItem(newItem);
				effectiveSize = lightfields.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, lightfields.size() - 1, lightfields.size() - 1);
			}
		});
		lightfields.add(newItem);
		return newItem;
	}

	@Override
	public LFDrawable<ContextType> addMorphFromLFMFile(File lfmFile, LFLoadOptions loadOptions) throws IOException 
	{
		LFDrawable<ContextType> newItem = this.createMorphFromLFMFile(lfmFile, loadOptions);
		newItem.setOnLoadCallback(new LFLoadingMonitor()
		{
			@Override
			public void startLoading()
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.startLoading();
				}
			}

			@Override
			public void setMaximum(double maximum)
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setMaximum(maximum);
				}
			}

			@Override
			public void setProgress(double progress) 
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setProgress(progress);
				}
			}
			
			@Override
			public void loadingComplete()
			{
				lightfields.setSelectedItem(newItem);
				effectiveSize = lightfields.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, lightfields.size() - 1, lightfields.size() - 1);
			}
		});
		lightfields.add(newItem);
		return newItem;
	}

	@Override
	public int getSize() 
	{
		return this.effectiveSize;
	}

	@Override
	public LFDrawable<ContextType> getElementAt(int index) 
	{
		return lightfields.get(index);
	}

	@Override
	public LFDrawable<ContextType> getSelectedItem() 
	{
		return lightfields.getSelectedItem();
	}

	@Override
	public void setSelectedItem(Object item) 
	{
		if (item == null)
		{
			lightfields.setSelectedItem(null);
		}
		else
		{
			for (int i = 0; i < lightfields.size(); i++)
			{
				if (lightfields.get(i) == item)
				{
					lightfields.setSelectedIndex(i);
				}
			}
		}
		this.fireContentsChanged(this, -1, -1);
	}
	
	@Override
	public void setLoadingMonitor(LFLoadingMonitor loadingMonitor)
	{
		this.loadingMonitor = loadingMonitor;
	}
	
	/**
	 * Gets a master Drawable entity that can be used to simultaneously manage all currently loaded light fields and render the currently active one.
	 * @return The master Drawable entity.
	 */
	public Drawable getDrawable()
	{
		return lightfields;
	}
}
