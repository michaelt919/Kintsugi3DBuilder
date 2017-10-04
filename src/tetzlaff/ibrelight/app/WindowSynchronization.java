package tetzlaff.ibrelight.app;//Created by alexk on 8/11/2017.

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

        // Make sure that multiple focus-related methods don't run at the same time.
        synchronized (this)
        {
            if (inFocusMethod)
            {
                alreadyRunning = true;
            }
            else
            {
                alreadyRunning = false;
                inFocusMethod = true;
            }
        }

        if (!alreadyRunning)
        {
            //System.out.println(trigger + ": Started focusGained()");

            if (!globalFocus)
            {
                // Make sure that the application really doesn't have focus (could conceivably occur due to some race condition).
                boolean focus = false;
                for (SynchronizedWindow listener : listeners)
                {
                    focus = focus || (!Objects.equals(listener, trigger) && listener.isFocused());
                }

                globalFocus = focus;
            }

            // Don't do anything if the application already had focus.
            if (!globalFocus && ChronoUnit.MILLIS.between(lastFocusLost, Instant.now()) >= 1000)
                // ^ 1000 ms = arbitrary threshold for distinguishing whether the application as a whole lost focus for a long enough duration.
                // TODO this threshold might need to be tweaked.
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

            //System.out.println(trigger + ": Ended focusGained()");

            synchronized (this)
            {
                inFocusMethod = false;
            }
        }
    }

    public void focusLost(SynchronizedWindow trigger)
    {
        boolean alreadyRunning;

        // Make sure that multiple focus-related methods don't run at the same time.
        synchronized (this)
        {
            if (inFocusMethod)
            {
                alreadyRunning = true;
            }
            else
            {
                alreadyRunning = false;
                inFocusMethod = true;
            }
        }

        if (!alreadyRunning)
        {
            //System.out.println(trigger + ": Started focusLost()");

            // Determine if the application as a whole has lost focus.
            boolean focus = false;
            for (SynchronizedWindow listener : listeners)
            {
                focus = focus || (!Objects.equals(listener, trigger) && listener.isFocused());
            }

            if (!focus)
            {
                this.lastFocusLost = Instant.now();
            }

            this.globalFocus = focus;

            //System.out.println(trigger + ": Ended focusLost()");

            synchronized (this)
            {
                inFocusMethod = false;
            }
        }
    }

    public void quit()
    {
        new Thread(() ->
        {
            boolean quitConfirmed = true;
            for (SynchronizedWindow listener : listeners)
            {
                quitConfirmed = quitConfirmed && listener.confirmQuit();
            }

            if (quitConfirmed)
            {
                for (SynchronizedWindow listener : listeners)
                {
                    listener.quit();
                }
            }
        })
        .start();
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
