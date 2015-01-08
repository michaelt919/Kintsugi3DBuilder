package tetzlaff.helpers;

public interface Selectable<T>
{
	int getSelectedIndex();
	T getSelectedItem();
	void setSelectedIndex(int index);
	void setSelectedItem(Object item);
}
