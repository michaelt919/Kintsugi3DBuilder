/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.app.logging;

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

import java.io.PrintWriter;
import java.io.StringWriter;
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
    private static final Logger logger = LoggerFactory.getLogger(RecentLogMessageAppender.class);
    private static final int MAX_MESSAGES = 2000;
    private static final int MESSAGE_TRUNC_SIZE = 10;
    private static RecentLogMessageAppender INSTANCE;
    private final ObservableList<LogMessage> messages = FXCollections.observableArrayList();
    private final List<LogMessageListener> listeners = new ArrayList<>();
    private final PatternLayout layout;

    protected RecentLogMessageAppender(String name, Filter filter, PatternLayout layout)
    {
        super(name, filter, layout);
        this.layout = layout;
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

        messages.add(message);
        dispatchEvents(message);
        clearOldMessages();
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
        return logger.isEnabledForLevel(level);
    }

    private void clearOldMessages()
    {
        if (messages.size() > MAX_MESSAGES)
        {
            messages.remove(0, MESSAGE_TRUNC_SIZE);
        }
    }

    private void dispatchEvents(LogMessage message)
    {
        for (LogMessageListener listener : listeners)
        {
            listener.newLogMessage(message);
        }
    }
}
