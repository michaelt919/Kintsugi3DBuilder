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
        //TODO
    }

    private void dispatchEvents(LogMessage message)
    {
        for (LogMessageListener listener : listeners)
        {
            listener.newLogMessage(message);
        }
    }
}
