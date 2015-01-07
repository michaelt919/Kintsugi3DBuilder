package tetzlaff.gl.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MultiDrawable<T extends Drawable> implements Drawable, List<T>
{
	private List<T> drawables;
	private int activeIndex;
	
	private List<T> removedDrawables;
	private List<T> addedDrawables;

	public MultiDrawable() 
	{
		drawables = new ArrayList<T>();
		removedDrawables = new ArrayList<T>();
		addedDrawables = new ArrayList<T>();
	}
	
	public MultiDrawable(List<T> drawables)
	{
		this.drawables = drawables;
		removedDrawables = new ArrayList<T>();
		addedDrawables = new ArrayList<T>(drawables);
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
	public void update() 
	{
		for (Drawable d : removedDrawables)
		{
			d.cleanup();
		}
		
		for (Drawable d : addedDrawables)
		{
			d.initialize();
		}
		
		removedDrawables = new ArrayList<T>();
		addedDrawables = new ArrayList<T>();
		
		for (Drawable d : drawables)
		{
			d.update();
		}
	}

	@Override
	public void draw() 
	{
		this.getActiveDrawable().draw();
	}

	@Override
	public void cleanup() 
	{
		for (Drawable d : drawables)
		{
			d.cleanup();
		}
	}
	
	public int getActiveIndex()
	{
		return activeIndex;
	}
	
	public Drawable getActiveDrawable()
	{
		return drawables.get(activeIndex);
	}
	
	public void setActiveIndex(int index)
	{
		this.activeIndex = index;
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
	public <T> T[] toArray(T[] a) 
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
			if (index < activeIndex)
			{
				activeIndex += c.size();
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
		if (index < activeIndex)
		{
			activeIndex++;
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
		if (index < activeIndex)
		{
			activeIndex--;
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
