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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;

public class EventDispatcher<ListenerType, EventType> implements EventListeners<ListenerType>
{
    private static final Logger LOG = LoggerFactory.getLogger(EventDispatcher.class);

    private final Collection<ListenerType> listeners = Collections.synchronizedList(new ArrayList<>(1));
    private final BiConsumer<ListenerType, EventType> notifyMethod;

    public EventDispatcher(BiConsumer<ListenerType, EventType> notifyMethod)
    {
        this.notifyMethod = notifyMethod;
    }

    @Override
    public void addListener(ListenerType listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ListenerType listener)
    {
        listeners.remove(listener);
    }

    public void notifyListeners(EventType event)
    {
        synchronized(listeners)
        {
            for (ListenerType listener : listeners)
            {
                try
                {
                    notifyMethod.accept(listener, event);
                }
                catch (RuntimeException e)
                {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }
}
