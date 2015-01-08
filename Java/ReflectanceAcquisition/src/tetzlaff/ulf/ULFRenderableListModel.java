package tetzlaff.ulf;

import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.MultiDrawable;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;

public class ULFRenderableListModel extends AbstractListModel<UnstructuredLightField> implements ULFListModel
{
	private OpenGLContext context;
	private Trackball trackball;
	private MultiDrawable<ULFRenderable> ulfs;
	private int effectiveSize;
	
	public ULFRenderableListModel(OpenGLContext context, Trackball trackball) 
	{
		this.context = context;
		this.trackball = trackball;
		this.ulfs = new MultiDrawable<ULFRenderable>();
		this.effectiveSize = 0;
	}

	@Override
	public UnstructuredLightField addFromDirectory(String directoryPath) throws IOException
	{
		ULFRenderable newULF = new ULFRenderable(context, directoryPath, trackball);
		newULF.setOnLoadCallback(() -> 
		{
			ulfs.setSelectedItem(newULF);
			this.effectiveSize = ulfs.size();
			this.fireIntervalAdded(this, ulfs.size() - 1, ulfs.size() - 1);
		});
		ulfs.add(newULF);
		return newULF.getLightField();
	}

	@Override
	public int getSize() 
	{
		return this.effectiveSize;
	}

	@Override
	public UnstructuredLightField getElementAt(int index) 
	{
		return ulfs.get(index).getLightField();
	}

	@Override
	public UnstructuredLightField getSelectedItem() 
	{
		ULFRenderable renderable = ulfs.getSelectedItem();
		return renderable == null ? null : renderable.getLightField();
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
				if (ulfs.get(i).getLightField() == item)
				{
					ulfs.setSelectedIndex(i);
				}
			}
		}
		this.fireContentsChanged(this, -1, -1);
	}
	
	public Drawable getDrawable()
	{
		return ulfs;
	}
}
