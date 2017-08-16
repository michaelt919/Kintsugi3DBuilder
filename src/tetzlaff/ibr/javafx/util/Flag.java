package tetzlaff.ibr.javafx.util;//Created by alexk on 8/1/2017.

import javafx.event.EventHandler;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class Flag
{
    private boolean value;

    public Flag(boolean value)
    {
        this.value = value;
    }

    public boolean get()
    {
        return value;
    }

    public void set(boolean value)
    {
        this.value = value;
    }

    public final EventHandler<WindowEvent> SET_FALSE_EVENT = param ->
    {
        System.out.println("SET_FALSE_EVENT");
        value = false;
    };
    public final EventHandler<WindowEvent> SET_TRUE_EVENT = param -> value = true;

    public void addFalseToClose(Window window)
    {
        window.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, SET_FALSE_EVENT);
    }
}
