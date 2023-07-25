package tetzlaff.ibrelight.app.logging;

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

@Plugin(
        name = "RecentLogMessageAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE
)
public class RecentLogMessageAppender extends AbstractAppender
{
    private static RecentLogMessageAppender INSTANCE;
    private final List<String> messages = new ArrayList<>();
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

    public static RecentLogMessageAppender getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void append(LogEvent event)
    {
        messages.add(event.getMessage().getFormattedMessage());
        dispatchEvents(event);
        clearOldMessages();
    }

    public List<String> getMessages()
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

    private void clearOldMessages()
    {
        //TODO
    }

    private void dispatchEvents(LogEvent event)
    {
        for (LogMessageListener listener : listeners)
        {
            listener.newLogMessage(event);
        }
    }
}
