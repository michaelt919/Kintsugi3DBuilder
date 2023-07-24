package tetzlaff.ibrelight.app.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

public abstract class LogMessageListener
{
    public Level getMinLevel()
    {
        return Level.DEBUG;
    }

    public Level getMaxLevel()
    {
        return Level.FATAL;
    }

    public abstract void newLogMessage(LogEvent logEvent);
}
