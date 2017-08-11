package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.interactive.InteractiveRenderableList;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.ibr.LoadingMonitor;
import tetzlaff.ibr.rendering2.IBRLoadOptions;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.mvc.models.ReadonlyObjectModel;

public class ImageBasedRendererList<ContextType extends Context<ContextType>> extends AbstractListModel<IBRRenderable<ContextType>> implements IBRRenderableListModel<ContextType>
{
	private static final long serialVersionUID = 4167467314632694946L;
	
	protected final ContextType context;
	private ReadonlyObjectModel objectModel;
	private ReadonlyCameraModel cameraModel;
	private ReadonlyLightModel lightModel;
	private Program<ContextType> program;
	private InteractiveRenderableList<ContextType, IBRRenderable<ContextType>> renderableList;
	private int effectiveSize;
	private LoadingMonitor loadingMonitor;
	
	public ImageBasedRendererList(ContextType context, Program<ContextType> program) 
	{
		this.context = context;
		this.renderableList = new InteractiveRenderableList<ContextType, IBRRenderable<ContextType>>();
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
		for (IBRRenderable<ContextType> ulf : renderableList)
		{
			ulf.setProgram(program);
		}
	}

	@Override
	public IBRRenderable<ContextType> addFromVSETFile(String id, File vsetFile, IBRLoadOptions loadOptions) throws IOException
	{
		// id = vsetFile.getPath()
		
		IBRRenderable<ContextType> newItem = 
			new IBRImplementation<ContextType>(id, context, this.getProgram(),
				IBRResources.getBuilderForContext(this.context)
					.setLoadingMonitor(this.loadingMonitor)
					.setLoadOptions(loadOptions)
					.loadVSETFile(vsetFile));
		
		newItem.setObjectModel(this.objectModel);
		newItem.setCameraModel(this.cameraModel);
		newItem.setLightModel(this.lightModel);
		
		newItem.setOnLoadCallback(new LoadingMonitor()
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
				renderableList.setSelectedItem(newItem);
				effectiveSize = renderableList.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, renderableList.size() - 1, renderableList.size() - 1);
			}
		});
		renderableList.add(newItem);
		return newItem;
	}
	
	@Override
	public IBRRenderable<ContextType> addFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, IBRLoadOptions loadOptions) throws IOException
	{
		IBRRenderable<ContextType> newItem = 
			new IBRImplementation<ContextType>(id, context, this.getProgram(),
				IBRResources.getBuilderForContext(this.context)
					.setLoadingMonitor(this.loadingMonitor)
					.setLoadOptions(loadOptions)
					.loadAgisoftFiles(xmlFile, meshFile, undistortedImageDirectory));

		newItem.setObjectModel(this.objectModel);
		newItem.setCameraModel(this.cameraModel);
		newItem.setLightModel(this.lightModel);
		
		newItem.setOnLoadCallback(new LoadingMonitor()
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
				renderableList.setSelectedItem(newItem);
				effectiveSize = renderableList.size();
				if (loadingMonitor != null)
				{
					loadingMonitor.loadingComplete();
				}
				fireIntervalAdded(this, renderableList.size() - 1, renderableList.size() - 1);
			}
		});
		renderableList.add(newItem);
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
		return renderableList.get(index);
	}

	@Override
	public IBRRenderable<ContextType> getSelectedItem() 
	{
		return renderableList.getSelectedItem();
	}

	@Override
	public void setSelectedItem(Object item) 
	{
		if (item == null)
		{
			renderableList.setSelectedItem(null);
		}
		else
		{
			for (int i = 0; i < renderableList.size(); i++)
			{
				if (renderableList.get(i) == item)
				{
					renderableList.setSelectedIndex(i);
				}
			}
		}
		this.fireContentsChanged(this, -1, -1);
	}
	
	@Override
	public void setLoadingMonitor(LoadingMonitor loadingMonitor)
	{
		this.loadingMonitor = loadingMonitor;
	}
	
	public InteractiveRenderable<ContextType> getRenderable()
	{
		return renderableList;
	}
	
	@Override
	public void setObjectModel(ReadonlyObjectModel objectModel) 
	{
		this.objectModel = objectModel;
		for (IBRRenderable<?> renderable : renderableList)
		{
			renderable.setObjectModel(objectModel);
		}
	}

	@Override
	public void setCameraModel(ReadonlyCameraModel cameraModel) 
	{
		this.cameraModel = cameraModel;
		for (IBRRenderable<?> renderable : renderableList)
		{
			renderable.setCameraModel(cameraModel);
		}
	}

	@Override
	public void setLightModel(ReadonlyLightModel lightModel) 
	{
		this.lightModel = lightModel;
		for (IBRRenderable<?> renderable : renderableList)
		{
			renderable.setLightModel(lightModel);
		}
	}
}
