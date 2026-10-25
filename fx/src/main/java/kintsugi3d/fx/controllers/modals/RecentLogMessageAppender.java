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

package kintsugi3d.fx.controllers.modals;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

@Plugin(
    name = "RecentLogMessageAppender",
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE
)
public final class RecentLogMessageAppender extends LogMessageAppenderBase
{
    private static RecentLogMessageAppender instance;

    private final ObservableList<LogMessage> messages = FXCollections.observableArrayList();

    private RecentLogMessageAppender(String name, Filter filter, Layout<String> layout)
    {
        super(name, filter, layout);

        //noinspection AssignmentToStaticFieldFromInstanceMethod,ThisEscapedInObjectConstruction
        instance = this;
    }

    @PluginFactory
    public static RecentLogMessageAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("PatternLayout") PatternLayout layout)
    {
        return new RecentLogMessageAppender(name, filter, layout);
    }

    public static RecentLogMessageAppender getInstance()
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
            // Appending seems to work OK on a non-JavaFX thread.
            // (JavaFX may not know that the new message is there right away, but it doesn't invalidate any stale information JavaFX may be working with
            // as all the previously valid messages / indices are still there and valid).
            messages.add(message);

            if (messages.size() >= MAX_MESSAGES)
            {
                // Removing from the ObservableList needs to happen on the JavaFX thread.
                try
                {
                    Platform.runLater(() ->
                    {
                        synchronized (messages)
                        {
                            // Make sure there is still something to remove.
                            // (If multiple messages came in between JavaFX frames they may have all triggered this
                            // but there might not be that many messages to remove.)
                            if (messages.size() >= MAX_MESSAGES)
                            {
                                messages.remove(0, MESSAGE_TRUNC_SIZE);
                            }
                        }
                    });
                }
                // TODO: Duktape fix to get around hardcoded JavaFX
                catch (IllegalStateException e)
                {
                    if (messages.size() >= MAX_MESSAGES)
                    {
                        messages.remove(0, MESSAGE_TRUNC_SIZE);
                    }
                }
            }
        }

        dispatchEvents(message);
    }

    @Override
    public ObservableList<LogMessage> getMessages()
    {
        return messages;
    }

}
