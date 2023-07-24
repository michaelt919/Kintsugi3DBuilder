package tetzlaff.ibrelight.app.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Plugin(
        name = "RecentLogMessageAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE
)
public class RecentLogMessageAppender extends AbstractAppender
{
    private static RecentLogMessageAppender INSTANCE;
    private final List<LogEvent> eventCache = new ArrayList<>();
    private final List<LogMessageListener> listeners = new ArrayList<>();

    protected RecentLogMessageAppender(String name, Filter filter)
    {
        super(name, filter, null);
        INSTANCE = this;
    }

    @PluginFactory
    public static RecentLogMessageAppender createAppender(
            @PluginAttribute("name") String name
    )
    {
        return new RecentLogMessageAppender(name, null);
    }

    public RecentLogMessageAppender getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void append(LogEvent event)
    {
        System.out.println("Log message received!");
        eventCache.add(event);
        dispatchEvents(event);
        clearOldMessages();
    }

    private void clearOldMessages()
    {
        //TODO
    }

    private void dispatchEvents(LogEvent event)
    {
        for (LogMessageListener listener : listeners)
        {
            if (event.getLevel().isInRange(listener.getMinLevel(), listener.getMaxLevel()))
            {
                listener.newLogMessage(event);
            }
        }
    }
}
