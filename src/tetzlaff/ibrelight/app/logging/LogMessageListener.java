package tetzlaff.ibrelight.app.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

public abstract class LogMessageListener
{
    public abstract void newLogMessage(LogMessage logMessage);
}
