package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.MultiDrawable;

public abstract class ULFDrawableListModel<ContextType extends Context<ContextType>> extends AbstractListModel<ULFDrawable<ContextType>> implements ULFListModel<ContextType>
{
	private static final long serialVersionUID = 4167467314632694946L;
	
	protected final ContextType context;
	protected final CameraController cameraController;
	private Program<ContextType> program;
	private Program<ContextType> indexProgram;
	private MultiDrawable<ULFDrawable<ContextType>> ulfs;
	private int effectiveSize;
	private ULFLoadingMonitor loadingMonitor;
	
	protected ULFDrawableListModel(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, CameraController cameraController) 
	{
		this.context = context;
		this.cameraController = cameraController;
		this.ulfs = new MultiDrawable<ULFDrawable<ContextType>>();
		this.effectiveSize = 0;
		
		this.program = program;
		this.indexProgram = indexProgram;
	}
	
	protected abstract ULFDrawable<ContextType> createFromVSETFile(File vsetFilee, ULFLoadOptions loadOptions) throws IOException;
	protected abstract ULFDrawable<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException;
	protected abstract ULFDrawable<ContextType> createMorphFromLFMFile(File lfmFilee, ULFLoadOptions loadOptions) throws IOException;
	
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

	public Program<ContextType> getIndexProgram()
	{
		return this.indexProgram;
	}
	
	public void setIndexProgram(Program<ContextType> program)
	{
		this.indexProgram = program;
		for (ULFDrawable<ContextType> ulf : ulfs)
		{
			ulf.setIndexProgram(program);
		}
	}
	
	protected CameraController getCameraController()
	{
		return this.cameraController;
	}

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
	
	public Drawable getDrawable()
	{
		return ulfs;
	}
}
