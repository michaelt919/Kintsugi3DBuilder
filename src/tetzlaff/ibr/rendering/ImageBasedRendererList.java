package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.interactive.InteractiveRenderableList;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRLoadingMonitor;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;

public class ImageBasedRendererList<ContextType extends Context<ContextType>> extends AbstractListModel<IBRRenderable<ContextType>> implements IBRRenderableListModel<ContextType>
{
	private static final long serialVersionUID = 4167467314632694946L;
	
	protected final ContextType context;
	protected final ReadonlyCameraModel cameraModel;
	protected final ReadonlyLightModel lightModel;
	private Program<ContextType> program;
	private InteractiveRenderableList<ContextType, IBRRenderable<ContextType>> ulfs;
	private int effectiveSize;
	private IBRLoadingMonitor loadingMonitor;
	
	public ImageBasedRendererList(ContextType context, Program<ContextType> program, ReadonlyCameraModel cameraModel, ReadonlyLightModel lightModel) 
	{
		this.context = context;
		this.cameraModel = cameraModel;
		this.lightModel = lightModel;
		this.ulfs = new InteractiveRenderableList<ContextType, IBRRenderable<ContextType>>();
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
		for (IBRRenderable<ContextType> ulf : ulfs)
		{
			ulf.setProgram(program);
		}
	}

	@Override
	public IBRRenderable<ContextType> addFromVSETFile(String id, File vsetFile, IBRLoadOptions loadOptions) throws IOException
	{
		// id = vsetFile.getPath()
		
		IBRRenderable<ContextType> newItem = 
			new IBRImplementation<ContextType>(id, context, this.getProgram(), this.cameraModel, lightModel,
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
	public IBRRenderable<ContextType> addFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, IBRLoadOptions loadOptions) throws IOException
	{
		IBRRenderable<ContextType> newItem = 
			new IBRImplementation<ContextType>(id, context, this.getProgram(), this.cameraModel, lightModel,
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
	public IBRRenderable<ContextType> getElementAt(int index) 
	{
		return ulfs.get(index);
	}

	@Override
	public IBRRenderable<ContextType> getSelectedItem() 
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
	
	public InteractiveRenderable<ContextType> getRenderable()
	{
		return ulfs;
	}
}
