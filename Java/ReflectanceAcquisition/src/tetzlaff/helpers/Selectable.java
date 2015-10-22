package tetzlaff.helpers;

/**
 * An interface for a collection of items of which one can be selected.
 * @author Michael Tetzlaff
 *
 * @param <T> The type of objects in this collection.
 */
public interface Selectable<T>
{
	/**
	 * Gets the index of the currently selected item.
	 * @return The index of the selected item.
	 */
	int getSelectedIndex();
	
	/**
	 * Gets the currently selected item.
	 * @return The selected item.
	 */
	T getSelectedItem();
	
	/**
	 * Sets the index of the currently selected item.
	 * @param index The index of the selected item.
	 */
	void setSelectedIndex(int index);
	
	/**
	 * Sets which item should be currently selected.
	 * If this exact item is not in the collection, this method has no effect.
	 * @param item The item to be selected.
	 */
	void setSelectedItem(Object item);
}
