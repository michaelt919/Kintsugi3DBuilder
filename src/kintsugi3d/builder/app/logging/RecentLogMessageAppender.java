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

package kintsugi3d.builder.app.logging;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Plugin(
        name = "RecentLogMessageAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE
)
public class RecentLogMessageAppender extends AbstractAppender
{
    private static final Logger LOG = LoggerFactory.getLogger(RecentLogMessageAppender.class);
    private static final int MAX_MESSAGES = 2000;
    private static final int MESSAGE_TRUNC_SIZE = 10;
    private static RecentLogMessageAppender INSTANCE;
    private final ObservableList<LogMessage> messages = FXCollections.observableArrayList();
    private final List<LogMessageListener> listeners = new ArrayList<>();

    protected RecentLogMessageAppender(String name, Filter filter, PatternLayout layout)
    {
        super(name, filter, layout);
        INSTANCE = this;
    }

    @PluginFactory
    public static RecentLogMessageAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("PatternLayout") PatternLayout layout
            )
    {
        return new RecentLogMessageAppender(name, filter, layout);
    }

    public static RecentLogMessageAppender getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void append(LogEvent event)
    {
        LogMessage message = new LogMessage(
                Instant.ofEpochMilli(event.getTimeMillis()),
                Level.valueOf(event.getLevel().name()),
                event.getLoggerName(),
                event.getMessage().getFormattedMessage(),
                event.getThrown()
        );

        synchronized(messages)
        {
            // Appending seems to work OK on a non-JavaFX thread.
            // (JavaFX may not know that the new message is there right away, but it doesn't invalidate any stale information JavaFX may be working with
            // as all the previously valid messages / indices are still there and valid).
            messages.add(message);

            if (messages.size() >= MAX_MESSAGES)
            {
                // Removing from the ObservableList needs to happen on the JavaFX thread.
                Platform.runLater(() ->
                {
                    synchronized (messages)
                    {
                        messages.remove(0, MESSAGE_TRUNC_SIZE);
                    }
                });
            }
        }

        dispatchEvents(message);
    }

    public ObservableList<LogMessage> getMessages()
    {
        return messages;
    }

    public void addListener(LogMessageListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(LogMessageListener listener)
    {
        listeners.remove(listener);
    }

    public boolean isLevelAvailable(Level level)
    {
        return LOG.isEnabledForLevel(level);
    }

    private void dispatchEvents(LogMessage message)
    {
        for (LogMessageListener listener : listeners)
        {
            listener.newLogMessage(message);
        }
    }
}
