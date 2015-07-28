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

public abstract class ULFDrawableListModel<ContextType extends Context<ContextType>> extends AbstractListModel<ULFDrawable> implements ULFListModel
{
	private static final long serialVersionUID = 4167467314632694946L;
	
	protected final ContextType context;
	protected final Program<ContextType> program;
	protected final Trackball trackball;
	private MultiDrawable<ULFDrawable> ulfs;
	private int effectiveSize;
	private ULFLoadingMonitor loadingMonitor;
	
	public ULFDrawableListModel(ContextType context, Trackball trackball) 
	{
		this.context = context;
		this.trackball = trackball;
		this.ulfs = new MultiDrawable<ULFDrawable>();
		this.effectiveSize = 0;
		
		try
        {
    		this.program = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/ulr.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/ulr.frag"))
    				.createProgram();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        	throw new IllegalStateException("The shader program could not be initialized.", e);
        }
	}
	
	protected abstract ULFDrawable createFromVSETFile(File vsetFile) throws IOException;
	protected abstract ULFRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, File imageDirectory) throws IOException;
	protected abstract ULFDrawable createMorphFromLFMFile(File lfmFile) throws IOException;

	@Override
	public ULFDrawable addFromVSETFile(File vsetFile) throws IOException
	{
		ULFDrawable newItem = this.createFromVSETFile(vsetFile);
		newItem.setOnLoadCallback(new ULFLoadingMonitor()
		{
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

			@Override
			public void setProgress(double progress) 
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setProgress(progress);
				}
			}
		});
		ulfs.add(newItem);
		return newItem;
	}
	
	@Override
	public ULFDrawable addFromAgisoftXMLFile(File xmlFile, File meshFile, File imageDirectory) throws IOException
	{
		ULFDrawable newItem = this.createFromAgisoftXMLFile(xmlFile, meshFile, imageDirectory);
		newItem.setOnLoadCallback(new ULFLoadingMonitor()
		{
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

			@Override
			public void setProgress(double progress) 
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setProgress(progress);
				}
			}
		});
		ulfs.add(newItem);
		return newItem;
	}

	@Override
	public ULFDrawable addMorphFromLFMFile(File lfmFile) throws IOException 
	{
		ULFDrawable newItem = this.createMorphFromLFMFile(lfmFile);
		newItem.setOnLoadCallback(new ULFLoadingMonitor()
		{
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

			@Override
			public void setProgress(double progress) 
			{
				if (loadingMonitor != null)
				{
					loadingMonitor.setProgress(progress);
				}
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
	public ULFDrawable getElementAt(int index) 
	{
		return ulfs.get(index);
	}

	@Override
	public ULFDrawable getSelectedItem() 
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
	
	public Drawable getDrawable()
	{
		return ulfs;
	}
}
