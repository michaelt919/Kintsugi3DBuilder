package tetzlaff.gl.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import tetzlaff.helpers.SelectableList;

/**
 * A container that can hold many Drawable objects with one of them being selected/active. It
 * is also itself a Drawable and will keep all its members updated and will draw the currently
 * 'active' Drawable in the collection.
 *
 * @param <T> The type of the objects stored in the container (must implement Drawable)
 * @see Drawable
 * @author Michael Tetzlaff
 */
public class MultiDrawable<T extends Drawable> implements Drawable, SelectableList<T>
{
	private List<T> drawables;
	private int selectedIndex = -1;
	
	private List<T> removedDrawables;
	private List<T> addedDrawables;
	
	private Exception initError;

	public MultiDrawable() 
	{
		drawables = new ArrayList<T>();
		removedDrawables = new ArrayList<T>();
		addedDrawables = new ArrayList<T>();
		initError = null;
	}
	
	public MultiDrawable(List<T> drawables)
	{
		this.drawables = drawables;
		removedDrawables = new ArrayList<T>();
		addedDrawables = new ArrayList<T>(drawables);
		initError = null;
	}
	
	@Override
	public void initialize() 
	{
		for (Drawable d : drawables)
		{
			d.initialize();
		}
	}

	@Override
	public boolean hasInitializeError()
	{
		return (initError != null);
	}

	@Override
	public Exception getInitializeError()
	{
		Exception tempRef = initError;
		initError = null;
		return tempRef;
	}
	
	@Override
	public void update() 
	{
		for (Drawable d : removedDrawables)
		{
			d.cleanup();
		}
		
		for (Drawable d : addedDrawables)
		{
			d.initialize();
			if(d.hasInitializeError())
			{
				initError = d.getInitializeError();
				d.cleanup();
			}
		}
		
		removedDrawables = new ArrayList<T>();
		addedDrawables = new ArrayList<T>();
		
		for (Drawable d : drawables)
		{
			if(!d.hasInitializeError())
			{
				d.update();
			}
		}
	}

	@Override
	public void draw() 
	{
		Drawable selected = this.getSelectedItem();
		if (selected != null && !selected.hasInitializeError())
		{
			selected.draw();
		}
	}

	@Override
	public void saveToFile(String fileFormat, File file)
	{
		Drawable selected = this.getSelectedItem();
		if (selected != null && !selected.hasInitializeError())
		{
			selected.saveToFile(fileFormat, file);
		}		
	}
	
	@Override
	public void cleanup() 
	{
		for (Drawable d : drawables)
		{
			d.cleanup();
		}
	}
	
	@Override
	public int getSelectedIndex()
	{
		return selectedIndex;
	}
	
	@Override
	public T getSelectedItem()
	{
		if (this.getSelectedIndex() < 0)
		{
			return null;
		}
		else
		{
			return drawables.get(selectedIndex);
		}
	}
	
	@Override
	public void setSelectedIndex(int index)
	{
		this.selectedIndex = index;
	}
	
	@Override
	public void setSelectedItem(Object item)
	{
		if (item == null)
		{
			this.setSelectedIndex(-1);
		}
		else
		{
			this.setSelectedIndex(drawables.indexOf(item));
		}
	}

	@Override
	public int size() 
	{
		return drawables.size();
	}

	@Override
	public boolean isEmpty() 
	{
		return drawables.isEmpty();
	}

	@Override
	public boolean contains(Object o) 
	{
		return drawables.contains(o);
	}

	@Override
	public Iterator<T> iterator() 
	{
		return drawables.iterator();
	}

	@Override
	public Object[] toArray() 
	{
		return drawables.toArray();
	}

	@Override
	public <U> U[] toArray(U[] a) 
	{
		return drawables.toArray(a);
	}

	@Override
	public boolean add(T e) 
	{
		if (drawables.add(e))
		{
			addedDrawables.add(e);
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) 
	{
		if (drawables.remove(o))
		{
			removedDrawables.add((T)o);
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) 
	{
		return drawables.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) 
	{
		if (drawables.addAll(c))
		{
			addedDrawables.addAll(c);
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) 
	{
		if (drawables.addAll(index, c))
		{
			addedDrawables.addAll(c);
			if (index < selectedIndex)
			{
				selectedIndex += c.size();
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) 
	{
		throw new UnsupportedOperationException("removeAll() is not supported by MultiDrawable.");
	}

	@Override
	public boolean retainAll(Collection<?> c) 
	{
		throw new UnsupportedOperationException("retainAll() is not supported by MultiDrawable.");
	}

	@Override
	public void clear() 
	{
		this.removedDrawables = this.drawables;
		this.drawables = new ArrayList<T>();
	}

	@Override
	public T get(int index) 
	{
		return drawables.get(index);
	}

	@Override
	public T set(int index, T element) 
	{
		T removed = drawables.set(index, element);
		if (removed != null)
		{
			removedDrawables.add(removed);
		}
		if (element != null)
		{
			addedDrawables.add(element);
		}
		return removed;
	}

	@Override
	public void add(int index, T element) 
	{
		drawables.add(index, element);
		if (element != null)
		{
			addedDrawables.add(element);
		}
		if (index < selectedIndex)
		{
			selectedIndex++;
		}
	}

	@Override
	public T remove(int index) 
	{
		T removed = drawables.remove(index);
		if (removed != null)
		{
			removedDrawables.add(removed);
		}
		if (index < selectedIndex)
		{
			selectedIndex--;
		}
		return removed;
	}

	@Override
	public int indexOf(Object o) 
	{
		return drawables.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) 
	{
		return drawables.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() 
	{
		return drawables.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) 
	{
		return drawables.listIterator(index);
	}

	@Override
	public MultiDrawable<T> subList(int fromIndex, int toIndex)
	{
		return new MultiDrawable<T>(drawables.subList(fromIndex, toIndex));
	}
}
