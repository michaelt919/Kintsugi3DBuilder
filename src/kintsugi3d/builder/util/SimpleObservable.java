/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SimpleObservable<K, V> implements Observable<MappedChange<K, V>>
{
    private final Collection<Observer<MappedChange<K, V>>> observers =
        Collections.synchronizedList(new ArrayList<>(8));

    @Override
    public void registerObserver(Observer<MappedChange<K, V>> observer)
    {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer<MappedChange<K, V>> observer)
    {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(MappedChange<K, V> change)
    {
        for (Observer<MappedChange<K, V>> observer : observers)
        {
            observer.update(change);
        }
    }
}