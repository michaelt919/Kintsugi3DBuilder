package tetzlaff.ulf;

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
 * An abstract class defining the skeleton of an implementation of a ULFListModel as a list of ULFDrawable entities.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that will be used for rendering.
 */
public abstract class ULFDrawableListModel<ContextType extends Context<ContextType>> extends AbstractListModel<ULFDrawable<ContextType>> implements ULFListModel<ContextType>
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
	
	
	private MultiDrawable<ULFDrawable<ContextType>> ulfs;
	private int effectiveSize;
	private ULFLoadingMonitor loadingMonitor;
	
	/**
	 * Creates a new ULFDrawableListModel.
	 * @param context The context that will be used for rendering.
	 * @param trackball The trackball controlling the movement of the virtual camera.
	 */
	public ULFDrawableListModel(ContextType context, Trackball trackball) 
	{
		this.context = context;
		this.trackball = trackball;
		this.ulfs = new MultiDrawable<ULFDrawable<ContextType>>();
		this.effectiveSize = 0;
		
		try
        {
			this.program = context.getShaderProgramBuilder()
					.addShader(ShaderType.VERTEX, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "ulr.vert"))
					.addShader(ShaderType.FRAGMENT, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "ulr.frag"))
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
		for (ULFDrawable<ContextType> ulf : ulfs)
		{
			ulf.setProgram(program);
		}
	}
	
	/**
	 * Required in order to define how to load an unstructured light field from a view set file.
	 * @param vsetFile The view set file defining the light field to be added.
	 * @param loadOptions The options to use when loading the light field.
	 * @return A new unstructured light field as a ULFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	protected abstract ULFDrawable<ContextType> createFromVSETFile(File vsetFile, ULFLoadOptions loadOptions) throws IOException;
	
	/**
	 * Required in order to define how to load an unstructured light field from Agisoft PhotoScan.
	 * @param xmlFile The Agisoft PhotoScan XML camera file defining the views of the light field to be added.
     * @param meshFile The mesh exported from Agisoft PhotoScan to be used as proxy geometry.
	 * @param loadOptions The options to use when loading the light field.
	 * @return A new unstructured light field as a ULFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	protected abstract ULFDrawable<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException;
	
	/**
	 * Required in order to define how to load an unstructured light field morph.
	 * @param lfmFile The light field morph file defining the morph to be added.
	 * @param loadOptions The options to use when loading the light field.
	 * @return A new unstructured light field morph as a ULFDrawable entity.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	protected abstract ULFDrawable<ContextType> createMorphFromLFMFile(File lfmFile, ULFLoadOptions loadOptions) throws IOException;

	@Override
	public ULFDrawable<ContextType> addFromVSETFile(File vsetFile, ULFLoadOptions loadOptions) throws IOException
	{
		ULFDrawable<ContextType> newItem = this.createFromVSETFile(vsetFile, loadOptions);
		newItem.setOnLoadCallback(new ULFLoadingMonitor()
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
				ulfs.setSelectedItem(newItem);
				effectiveSize = ulfs.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, ulfs.size() - 1, ulfs.size() - 1);
			}
		});
		ulfs.add(newItem);
		return newItem;
	}
	
	@Override
	public ULFDrawable<ContextType> addFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException
	{
		ULFDrawable<ContextType> newItem = this.createFromAgisoftXMLFile(xmlFile, meshFile, loadOptions);
		newItem.setOnLoadCallback(new ULFLoadingMonitor()
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
				ulfs.setSelectedItem(newItem);
				effectiveSize = ulfs.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, ulfs.size() - 1, ulfs.size() - 1);
			}
		});
		ulfs.add(newItem);
		return newItem;
	}

	@Override
	public ULFDrawable<ContextType> addMorphFromLFMFile(File lfmFile, ULFLoadOptions loadOptions) throws IOException 
	{
		ULFDrawable<ContextType> newItem = this.createMorphFromLFMFile(lfmFile, loadOptions);
		newItem.setOnLoadCallback(new ULFLoadingMonitor()
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
				ulfs.setSelectedItem(newItem);
				effectiveSize = ulfs.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, ulfs.size() - 1, ulfs.size() - 1);
			}
		});
		ulfs.add(newItem);
		return newItem;
	}

	@Override
	public int getSize() 
	{
		return this.effectiveSize;
	}

	@Override
	public ULFDrawable<ContextType> getElementAt(int index) 
	{
		return ulfs.get(index);
	}

	@Override
	public ULFDrawable<ContextType> getSelectedItem() 
	{
		return ulfs.getSelectedItem();
	}

	@Override
	public void setSelectedItem(Object item) 
	{
		if (item == null)
		{
			ulfs.setSelectedItem(null);
		}
		else
		{
			for (int i = 0; i < ulfs.size(); i++)
			{
				if (ulfs.get(i) == item)
				{
					ulfs.setSelectedIndex(i);
				}
			}
		}
		this.fireContentsChanged(this, -1, -1);
	}
	
	@Override
	public void setLoadingMonitor(ULFLoadingMonitor loadingMonitor)
	{
		this.loadingMonitor = loadingMonitor;
	}
	
	/**
	 * Gets a master Drawable entity that can be used to simultaneously manage all currently loaded light fields and render the currently active one.
	 * @return The master Drawable entity.
	 */
	public Drawable getDrawable()
	{
		return ulfs;
	}
}
