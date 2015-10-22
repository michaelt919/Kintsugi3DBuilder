package tetzlaff.helpers;

import java.util.List;

/**
 * An interface for a collection which combines the functionality of a List and a Selectable.
 * @author Michael Tetzlaff
 *
 * @param <T> The type of objects stored in this collection.
 */
public interface SelectableList<T> extends List<T>, Selectable<T>
{
}
