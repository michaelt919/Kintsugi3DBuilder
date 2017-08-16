package tetzlaff.interactive;

public interface EventPollable 
{
    void pollEvents();
    boolean shouldTerminate();
}
