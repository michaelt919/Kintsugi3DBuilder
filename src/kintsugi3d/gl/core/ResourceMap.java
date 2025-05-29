/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.core;

import java.util.*;

public class ResourceMap<K, V extends Resource> implements Resource, Map<K, V>
{
    private final Map<K, V> base;

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public ResourceMap()
    {
        base = new HashMap<>();
    }

    public ResourceMap(int initialCapacity)
    {
        base = new HashMap<>(initialCapacity);
    }

    public ResourceMap(int initialCapacity, int loadFactor)
    {
        base = new HashMap<>(initialCapacity, loadFactor);
    }

    @Override
    public int size()
    {
        return base.size();
    }

    @Override
    public boolean isEmpty()
    {
        return base.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return base.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return base.containsValue(value);
    }

    @Override
    public V get(Object key)
    {
        return base.get(key);
    }

    @Override
    public V put(K key, V value)
    {
        return base.put(key, value);
    }

    @Override
    public V remove(Object key)
    {
        get(key).close(); // close the resource before removing it from the map
        return base.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        base.putAll(m);
    }

    @Override
    public void clear()
    {
        // close all resources before clearing the map
        for (V resource : values())
        {
            resource.close();
        }

        base.clear();
    }

    @Override
    public Set<K> keySet()
    {
        return base.keySet();
    }

    @Override
    public Collection<V> values()
    {
        // add / remove via values() or entrySet() not allowed to simplify resource management
        return Collections.unmodifiableCollection(base.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        // add / remove via values() or entrySet() not allowed to simplify resource management
        return Collections.unmodifiableSet(base.entrySet());
    }

    @Override
    public void close()
    {
        for (V resource : values())
        {
            resource.close();
        }
    }
}
