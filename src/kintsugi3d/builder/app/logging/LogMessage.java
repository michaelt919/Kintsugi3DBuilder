package kintsugi3d.builder.app.logging;

import org.slf4j.event.Level;

import java.time.Instant;

public class LogMessage
{
    private final Instant timestamp;
    private final Level logLevel;
    private final String sourceClassName;
    private final String message;

    private final Throwable thrown;

    public LogMessage(Instant timestamp, Level logLevel, String sourceClassName, String message)
    {
        this.timestamp = timestamp;
        this.logLevel = logLevel;
        this.sourceClassName = sourceClassName;
        this.message = message;
        this.thrown = null;
    }

    public LogMessage(Instant timestamp, Level logLevel, String sourceClassName, String message, Throwable thrown)
    {
        this.timestamp = timestamp;
        this.logLevel = logLevel;
        this.sourceClassName = sourceClassName;
        this.message = message;
        this.thrown = thrown;
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

    public Throwable getThrown()
    {
        return thrown;
    }
}
