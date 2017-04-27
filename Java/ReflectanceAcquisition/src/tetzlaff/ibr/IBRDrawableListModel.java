package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.MultiDrawable;

public abstract class IBRDrawableListModel<ContextType extends Context<ContextType>> extends AbstractListModel<IBRDrawable<ContextType>> implements IBRListModel<ContextType>
{
	private static final long serialVersionUID = 4167467314632694946L;
	
	protected final ContextType context;
	protected final CameraController cameraController;
	private Program<ContextType> program;
	private MultiDrawable<IBRDrawable<ContextType>> ulfs;
	private int effectiveSize;
	private IBRLoadingMonitor loadingMonitor;
	
	protected IBRDrawableListModel(ContextType context, Program<ContextType> program, CameraController cameraController) 
	{
		this.context = context;
		this.cameraController = cameraController;
		this.ulfs = new MultiDrawable<IBRDrawable<ContextType>>();
		this.effectiveSize = 0;
		
		this.program = program;
	}
	
	protected abstract IBRDrawable<ContextType> createFromVSETFile(File vsetFilee, IBRLoadOptions loadOptions) throws IOException;
	protected abstract IBRDrawable<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions) throws IOException;
	
	public Program<ContextType> getProgram()
	{
		return this.program;
	}
	
	public void setProgram(Program<ContextType> program)
	{
		this.program = program;
		for (IBRDrawable<ContextType> ulf : ulfs)
		{
			ulf.setProgram(program);
		}
	}
	
	protected CameraController getCameraController()
	{
		return this.cameraController;
	}

	@Override
	public IBRDrawable<ContextType> addFromVSETFile(File vsetFile, IBRLoadOptions loadOptions) throws IOException
	{
		IBRDrawable<ContextType> newItem = this.createFromVSETFile(vsetFile, loadOptions);
		newItem.setOnLoadCallback(new IBRLoadingMonitor()
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
	public IBRDrawable<ContextType> addFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions) throws IOException
	{
		IBRDrawable<ContextType> newItem = this.createFromAgisoftXMLFile(xmlFile, meshFile, loadOptions);
		newItem.setOnLoadCallback(new IBRLoadingMonitor()
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
	public IBRDrawable<ContextType> getElementAt(int index) 
	{
		return ulfs.get(index);
	}

	@Override
	public IBRDrawable<ContextType> getSelectedItem() 
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
	public void setLoadingMonitor(IBRLoadingMonitor loadingMonitor)
	{
		this.loadingMonitor = loadingMonitor;
	}
	
	public Drawable getDrawable()
	{
		return ulfs;
	}
}
