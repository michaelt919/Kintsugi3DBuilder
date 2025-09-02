package kintsugi3d.builder.javafx.internal;

import javafx.event.Event;
import javafx.event.EventType;

public class ProcessingCompleteEvent extends Event
{
    public static final EventType<ProcessingCompleteEvent> PROCESSING_COMPLETE
        = new EventType<>(Event.ANY, "PROCESSING COMPLETE");

    public ProcessingCompleteEvent()
    {
        super(PROCESSING_COMPLETE);
    }
}
