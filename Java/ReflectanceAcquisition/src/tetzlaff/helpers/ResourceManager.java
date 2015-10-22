package tetzlaff.helpers;

import tetzlaff.gl.exceptions.NoAvailableTextureUnitsException;

/**
 * A class for managing a collection of resources that are to be accessed by some "key" but must be packed into a finite number of units.
 * Once assigned, a unit is bound to a key permanently; it cannot be reassigned.
 * @author Michael Tetzlaff
 *
 * @param <ResourceType> The type of resource to be managed.
 */
public class ResourceManager<ResourceType>
{
	/**
	 * The maximum number of resources that can be managed - that is, the number of units available.
	 */
	public final int length;
	
	/**
	 * The keys associated with each resource unit.
	 */
	private int[] keys;
	
	/**
	 * The resources assigned to each unit.
	 */
	private Object[] resources;
	
	/**
	 * The index of the next available resource slot.
	 */
	private int nextSlot;

	/**
	 * Creates a new resource manager.
	 * @param length The maximum number of resources that can be managed - that is, the number of units available.
	 */
	public ResourceManager(int length) 
	{
		this.length = length;
		keys = new int[length];
		for (int i = 0; i < length; i++)
		{
			keys[i] = -1;
		}
		resources = new Object[length];
		nextSlot = 0;
	}

	/**
	 * Assigns a resource to a particular key.
	 * If the key has already been associated with a particular resource unit, the existing resource bound to that key will be replaced and the resource unit will be reused.
	 * Otherwise, the resource manager will search for a free resource unit to associate with the key and assign the resource to that unit.
	 * A runtime exception will be thrown if no available resource unit can be found.
	 * @param key The key to assign the resource to.
	 * @param resource The resource to be assigned.
	 * @return The index of the resource unit being used by the specified resource.
	 */
	public int assignResourceByKey(int key, ResourceType resource)
	{
		// Check if the key has already been assigned a texture
		for (int i = 0; i < length; i++)
		{
			if (keys[i] == key)
			{
				resources[i] = resource;
				return i;
			}
		}
		
		if (nextSlot == length)
		{
			// No more slots available.
			throw new NoAvailableTextureUnitsException("No more available resource slots.");
		}
		else
		{
			// The key has not been assigned a resource, so use the next available slot
			keys[nextSlot] = key;
			resources[nextSlot] = resource;
			return nextSlot++;
		}
	}

	@SuppressWarnings("unchecked")
	public ResourceType getResourceByUnit(int index)
	{
		return (ResourceType)resources[index];
	}
}
