package tetzlaff.ibr.app;//Created by alexk on 8/11/2017.

import java.util.Collection;
import java.util.LinkedList;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;


public final class Quit implements QuitListener, EventHandler<WindowEvent>
{
    private final Collection<QuitListener> quitListeners = new LinkedList<>();
    
    public void addQuitListener(QuitListener listener)
    {
        quitListeners.add(listener);
    }
    
    @Override
    public void handle(WindowEvent event) 
    {
        this.applicationQuitting();
    }
    
    @Override
    public void applicationQuitting() 
    {
        for(QuitListener listener : quitListeners)
        {
            listener.applicationQuitting();
        }
    }

    private Quit() 
    {
    }
    
    private static final Quit INSTANCE = new Quit();
    
    public static Quit getInstance() 
    {
        return INSTANCE;
    }
}
