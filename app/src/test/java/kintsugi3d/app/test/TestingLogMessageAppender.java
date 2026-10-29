/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Kaelie Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday, Joseph O'Connor
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.app.test;

import kintsugi3d.builder.util.LogMessageAppenderBase;
import kintsugi3d.builder.util.logging.LogMessage;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.event.Level;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Plugin(
    name = "RecentLogMessageAppender",
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE
)
public final class TestingLogMessageAppender extends LogMessageAppenderBase
{
    private static TestingLogMessageAppender instance;

    private final List<LogMessage> messages = new ArrayList<>(16);

    private TestingLogMessageAppender(String name, Filter filter, Layout<String> layout)
    {
        super(name, filter, layout);

        //noinspection AssignmentToStaticFieldFromInstanceMethod,ThisEscapedInObjectConstruction
        instance = this;
    }

    @PluginFactory
    public static TestingLogMessageAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("PatternLayout") PatternLayout layout)
    {
        return new TestingLogMessageAppender(name, filter, layout);
    }

    public static TestingLogMessageAppender getInstance()
    {
        //noinspection StaticVariableUsedBeforeInitialization
        return instance;
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
            messages.add(message);

            if (messages.size() >= MAX_MESSAGES)
            {
                messages.subList(0, MESSAGE_TRUNC_SIZE).clear();
            }
        }

        dispatchEvents(message);
    }

    @Override
    public List<LogMessage> getMessages()
    {
        return messages;
    }
}
