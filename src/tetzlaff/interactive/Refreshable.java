package tetzlaff.interactive;

public interface Refreshable 
{
    void initialize() throws InitializationException;
    void refresh();
    void terminate();
}
