package tetzlaff.ibrelight.app.logging;

import org.slf4j.event.Level;

import java.time.Instant;

public class LogMessage
{
    private final Instant timestamp;
    private final Level logLevel;
    private final String sourceClassName;
    private final String message;

    public LogMessage(Instant timestamp, Level logLevel, String sourceClassName, String message)
    {
        this.timestamp = timestamp;
        this.logLevel = logLevel;
        this.sourceClassName = sourceClassName;
        this.message = message;
    }

    public Instant getTimestamp()
    {
        return timestamp;
    }

    public Level getLogLevel()
    {
        return logLevel;
    }

    public String getSourceClassName()
    {
        return sourceClassName;
    }

    public String getMessage()
    {
        return message;
    }
}
