/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.util;

import java.util.AbstractList;
import java.util.List;
import java.util.Optional;

import kintsugi3d.gl.exceptions.NoAvailableTextureUnitsException;

public class ResourceManager<ResourceType>
{
    public final int length;

    private final int[] keys;
    private final Object[] resources;
    private int nextSlot;

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
            int result = nextSlot;
            nextSlot++;
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public ResourceType getResourceByUnit(int index)
    {
        return (ResourceType)resources[index];
    }

    public List<Optional<ResourceType>> asReadonlyList()
    {
        return new AbstractList<Optional<ResourceType>>()
        {
            @Override
            public int size()
            {
                return length;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Optional<ResourceType> get(int index)
            {
                return Optional.ofNullable((ResourceType)resources[index]);
            }
        };
    }
}
