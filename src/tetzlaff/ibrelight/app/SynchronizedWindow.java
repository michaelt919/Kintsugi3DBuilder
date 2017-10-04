package tetzlaff.ibrelight.app;//Created by alexk on 8/11/2017.

public interface SynchronizedWindow
{
    boolean isFocused();
    void focus();
    void quit();

    default boolean confirmQuit()
    {
        return true;
    }
}
