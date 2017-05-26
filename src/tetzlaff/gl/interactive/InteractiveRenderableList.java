package tetzlaff.gl.interactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import tetzlaff.util.SelectableList;

/**
 * A container that can hold many InteractiveRenderable objects with one of them being selected/active. It
 * is also itself a InteractiveRenderable and will keep all its members updated and will draw the currently
 * 'active' InteractiveRenderable in the collection.
 *
 * @param <T> The type of the objects stored in the container (must implement InteractiveRenderable)
 * @see InteractiveRenderable
 * @author Michael Tetzlaff
 */
public class InteractiveRenderableList<T extends InteractiveRenderable> implements InteractiveRenderable, SelectableList<T>
{
	private List<T> renderables;
	private int selectedIndex = -1;
	
	private List<T> removedRenderables;
	private List<T> addedRenderables;

	public InteractiveRenderableList() 
	{
		renderables = new ArrayList<T>();
		removedRenderables = new ArrayList<T>();
		addedRenderables = new ArrayList<T>();
	}
	
	public InteractiveRenderableList(List<T> renderables)
	{
		this.renderables = renderables;
		removedRenderables = new ArrayList<T>();
		addedRenderables = new ArrayList<T>(renderables);
	}
	
	@Override
	public void initialize() 
	{
		for (InteractiveRenderable d : renderables)
		{
			d.initialize();
		}
	}

	@Override
	public void update() 
	{
		for (InteractiveRenderable d : removedRenderables)
		{
			d.close();
		}
		
		for (InteractiveRenderable d : addedRenderables)
		{
			d.initialize();
		}
		
		removedRenderables = new ArrayList<T>();
		addedRenderables = new ArrayList<T>();
		
		for (InteractiveRenderable d : renderables)
		{
			d.update();
		}
	}

	@Override
	public void draw() 
	{
		InteractiveRenderable selected = this.getSelectedItem();
		if (selected != null)
		{
			selected.draw();
		}
	}

	@Override
	public void close() 
	{
		for (InteractiveRenderable d : renderables)
		{
			d.close();
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
			return renderables.get(selectedIndex);
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
			this.setSelectedIndex(renderables.indexOf(item));
		}
	}

	@Override
	public int size() 
	{
		return renderables.size();
	}

	@Override
	public boolean isEmpty() 
	{
		return renderables.isEmpty();
	}

	@Override
	public boolean contains(Object o) 
	{
		return renderables.contains(o);
	}

	@Override
	public Iterator<T> iterator() 
	{
		return renderables.iterator();
	}

	@Override
	public Object[] toArray() 
	{
		return renderables.toArray();
	}

	@Override
	public <U> U[] toArray(U[] a) 
	{
		return renderables.toArray(a);
	}

	@Override
	public boolean add(T e) 
	{
		if (renderables.add(e))
		{
			addedRenderables.add(e);
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
		if (renderables.remove(o))
		{
			removedRenderables.add((T)o);
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
		return renderables.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) 
	{
		if (renderables.addAll(c))
		{
			addedRenderables.addAll(c);
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
		if (renderables.addAll(index, c))
		{
			addedRenderables.addAll(c);
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
		throw new UnsupportedOperationException("removeAll() is not supported by MultiRenderable.");
	}

	@Override
	public boolean retainAll(Collection<?> c) 
	{
		throw new UnsupportedOperationException("retainAll() is not supported by MultiRenderable.");
	}

	@Override
	public void clear() 
	{
		this.removedRenderables = this.renderables;
		this.renderables = new ArrayList<T>();
	}

	@Override
	public T get(int index) 
	{
		return renderables.get(index);
	}

	@Override
	public T set(int index, T element) 
	{
		T removed = renderables.set(index, element);
		if (removed != null)
		{
			removedRenderables.add(removed);
		}
		if (element != null)
		{
			addedRenderables.add(element);
		}
		return removed;
	}

	@Override
	public void add(int index, T element) 
	{
		renderables.add(index, element);
		if (element != null)
		{
			addedRenderables.add(element);
		}
		if (index < selectedIndex)
		{
			selectedIndex++;
		}
	}

	@Override
	public T remove(int index) 
	{
		T removed = renderables.remove(index);
		if (removed != null)
		{
			removedRenderables.add(removed);
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
		return renderables.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) 
	{
		return renderables.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() 
	{
		return renderables.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) 
	{
		return renderables.listIterator(index);
	}

	@Override
	public InteractiveRenderableList<T> subList(int fromIndex, int toIndex)
	{
		return new InteractiveRenderableList<T>(renderables.subList(fromIndex, toIndex));
	}
}
