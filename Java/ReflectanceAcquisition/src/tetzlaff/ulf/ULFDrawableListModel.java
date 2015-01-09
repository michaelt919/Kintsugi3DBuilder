package tetzlaff.ulf;

import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.MultiDrawable;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;

public abstract class ULFDrawableListModel<T extends ULFDrawable> extends AbstractListModel<UnstructuredLightField> implements ULFListModel
{
	protected final OpenGLContext context;
	protected final Trackball trackball;
	private MultiDrawable<T> ulfs;
	private int effectiveSize;
	
	public ULFDrawableListModel(OpenGLContext context, Trackball trackball) 
	{
		this.context = context;
		this.trackball = trackball;
		this.ulfs = new MultiDrawable<T>();
		this.effectiveSize = 0;
	}
	
	protected abstract T createFromDirectory(String directoryPath) throws IOException;

	@Override
	public UnstructuredLightField addFromDirectory(String directoryPath) throws IOException
	{
		T newItem = this.createFromDirectory(directoryPath);
		newItem.setOnLoadCallback(() -> 
		{
			ulfs.setSelectedItem(newItem);
			this.effectiveSize = ulfs.size();
			this.fireIntervalAdded(this, ulfs.size() - 1, ulfs.size() - 1);
		});
		ulfs.add(newItem);
		return newItem.getLightField();
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
		T selectedItem = ulfs.getSelectedItem();
		return selectedItem == null ? null : selectedItem.getLightField();
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
