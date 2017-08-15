package tetzlaff.ibr.app;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.javafx.JavaFXApp;

public class ThreadableUI implements Runnable 
{
    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() 
    {
        JavaFXApp.launchWrapper("");
    }
}
