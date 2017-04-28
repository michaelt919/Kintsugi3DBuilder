package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.MultiDrawable;
import tetzlaff.ibr.IBRDrawable;
import tetzlaff.ibr.IBRDrawableListModel;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRLoadingMonitor;

public class ImageBasedRendererList<ContextType extends Context<ContextType>> extends AbstractListModel<IBRDrawable<ContextType>> implements IBRDrawableListModel<ContextType>
{
	private static final long serialVersionUID = 4167467314632694946L;
	
	protected final ContextType context;
	protected final CameraController cameraController;
	protected final LightController lightController;
	private Program<ContextType> program;
	private MultiDrawable<IBRDrawable<ContextType>> ulfs;
	private int effectiveSize;
	private IBRLoadingMonitor loadingMonitor;
	
	public ImageBasedRendererList(ContextType context, Program<ContextType> program, CameraController cameraController, LightController lightController) 
	{
		this.context = context;
		this.cameraController = cameraController;
		this.lightController = lightController;
		this.ulfs = new MultiDrawable<IBRDrawable<ContextType>>();
		this.effectiveSize = 0;
		
		this.program = program;
	}
	
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
	public IBRDrawable<ContextType> addFromVSETFile(String id, File vsetFile, IBRLoadOptions loadOptions) throws IOException
	{
		// id = vsetFile.getPath()
		
		IBRDrawable<ContextType> newItem = 
			new ImageBasedRenderer<ContextType>(id, context, this.getProgram(), this.getCameraController(), lightController,
				IBRResources.getBuilderForContext(this.context)
					.setLoadingMonitor(this.loadingMonitor)
					.setLoadOptions(loadOptions)
					.loadVSETFile(vsetFile));
		
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
	public IBRDrawable<ContextType> addFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, IBRLoadOptions loadOptions) throws IOException
	{
		IBRDrawable<ContextType> newItem = 
			new ImageBasedRenderer<ContextType>(id, context, this.getProgram(), this.getCameraController(), lightController,
				IBRResources.getBuilderForContext(this.context)
					.setLoadingMonitor(this.loadingMonitor)
					.setLoadOptions(loadOptions)
					.loadAgisoftFiles(xmlFile, meshFile, undistortedImageDirectory));
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
