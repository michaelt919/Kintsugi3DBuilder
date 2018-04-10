package tetzlaff.ibrelight.app;

import java.io.IOException;
import java.io.PrintStream;

import tetzlaff.interactive.InitializationException;

public final class IBRelight
{
    private static final boolean DEBUG = true;

    private IBRelight()
    {
    }

    public static void main(String... args) throws IOException, InitializationException
    {
        if (!DEBUG)
        {
            PrintStream out = new PrintStream("out.log");
            PrintStream err = new PrintStream("err.log");
            System.setOut(out);
            System.setErr(err);
        }

        //allow render thread to modify user interface thread
        System.setProperty("glass.disableThreadChecks", "true");
        //TODO see com.sun.glass.ui.Application.java line 434

        System.out.println("Starting JavaFX UI");
        startJavaFXUI();

        System.out.println("Starting Render Window");
        startRenderWindow();

        System.out.println("Boot Complete");

    }

    private static void startJavaFXUI()
    {
        new Thread(new ThreadableUI()).start();
    }

    private static void startRenderWindow() throws InitializationException
    {
        Rendering.runProgram();
    }
}
