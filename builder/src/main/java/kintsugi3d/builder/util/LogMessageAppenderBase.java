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

import kintsugi3d.builder.util.logging.LogMessage;
import kintsugi3d.builder.util.logging.LogMessageListener;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class LogMessageAppenderBase extends AbstractAppender
{
    private static final Logger LOG = LoggerFactory.getLogger(LogMessageAppenderBase.class);
    public static final int MAX_MESSAGES = 2000;
    public static final int MESSAGE_TRUNC_SIZE = 10;
    private final Collection<LogMessageListener> listeners = new ArrayList<>(1);

    protected LogMessageAppenderBase(String name, Filter filter, Layout<? extends Serializable> layout)
    {
        super(name, filter, layout);
    }

    public static boolean isLevelAvailable(Level level)
    {
        return LOG.isEnabledForLevel(level);
    }

    public abstract List<LogMessage> getMessages();

    public void addListener(LogMessageListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(LogMessageListener listener)
    {
        listeners.remove(listener);
    }

    protected void dispatchEvents(LogMessage message)
    {
        for (LogMessageListener listener : listeners)
        {
            listener.newLogMessage(message);
        }
    }
}
