package tetzlaff.ibr.app;//Created by alexk on 8/11/2017.

import java.util.LinkedList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;


public class Quit implements QuitListener, EventHandler<WindowEvent> 
{
    private final List<QuitListener> quitTriggers = new LinkedList<>();
    
    public void addCloseTrigger(QuitListener trigger)
    {
        quitTriggers.add(trigger);
    }
    
    @Override
    public void handle(WindowEvent event) 
    {
        this.applicationQuitting();
    }
    
    @Override
    public void applicationQuitting() 
    {
        for(QuitListener trigger : quitTriggers)
        {
            trigger.applicationQuitting();
        }
    }

    private Quit() 
    {
    }
    
    private static final Quit instance = new Quit();
    
    public static Quit getInstance() 
    {
        return instance;
    }
}
