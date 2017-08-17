package tetzlaff.ibr.app;//Created by alexk on 8/11/2017.

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

public final class WindowSynchronization
{
    private final Collection<SynchronizedWindow> listeners = new LinkedList<>();

    private boolean globalFocus = true;
    
    public void addListener(SynchronizedWindow listener)
    {
        listeners.add(listener);
    }

    private boolean inFocusMethod = false;

    private Instant lastFocusLost;

    // TODO make focus more robust.  Need to think more about how things work with multithreading.
    public void focusGained(SynchronizedWindow trigger)
    {
        boolean alreadyRunning;

        synchronized (this)
        {
            if (!inFocusMethod)
            {
                alreadyRunning = false;
                inFocusMethod = true;
            }
            else
            {
                alreadyRunning = true;
            }
        }

        if (!alreadyRunning)
        {
            System.out.println(trigger + ": Started focusGained()");

            if (!globalFocus)
            {
                boolean focus = false;
                for (SynchronizedWindow listener : listeners)
                {
                    focus = focus || (!Objects.equals(listener, trigger) && listener.isFocused());
                    if (listener.isFocused())
                    {
                        System.out.println(listener);
                    }
                }

                globalFocus = focus;
            }

            if (!globalFocus && ChronoUnit.MILLIS.between(lastFocusLost, Instant.now()) >= 1000)
            {
                globalFocus = true;

                for (SynchronizedWindow listener : listeners)
                {
                    listener.focus();
                }
            }
            else
            {
                globalFocus = true;
            }

            System.out.println(trigger + ": Ended focusGained()");

            synchronized (this)
            {
                inFocusMethod = false;
            }
        }
    }

    public void focusLost(SynchronizedWindow trigger)
    {
        boolean alreadyRunning;

        synchronized (this)
        {
            if (!inFocusMethod)
            {
                alreadyRunning = false;
                inFocusMethod = true;
            }
            else
            {
                alreadyRunning = true;
            }
        }

        if (!alreadyRunning)
        {
            System.out.println(trigger + ": Started focusLost()");

            boolean focus = false;
            for (SynchronizedWindow listener : listeners)
            {
                focus = focus || (!Objects.equals(listener, trigger) && listener.isFocused());
                if (listener.isFocused())
                {
                    System.out.println(listener);
                }
            }

            if (!focus)
            {
                this.lastFocusLost = Instant.now();
            }

            this.globalFocus = focus;

            System.out.println(trigger + ": Ended focusLost()");

            synchronized (this)
            {
                inFocusMethod = false;
            }
        }
    }

    public void quit()
    {
        for(SynchronizedWindow listener : listeners)
        {
            listener.quit();
        }
    }

    private WindowSynchronization()
    {
    }
    
    private static final WindowSynchronization INSTANCE = new WindowSynchronization();
    
    public static WindowSynchronization getInstance()
    {
        return INSTANCE;
    }
}
