package tetzlaff.ibrelight.app;

import java.io.IOException;
import java.io.PrintStream;

import tetzlaff.ibrelight.javafx.MainApplication;
import tetzlaff.interactive.InitializationException;

public final class IBRelight
{
    private static final boolean DEBUG = false;
    private static final boolean GRAPHICS_WINDOW_ENABLED = false;

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

        if (GRAPHICS_WINDOW_ENABLED)
        {
            System.out.println("Starting JavaFX UI");
            new Thread(() -> MainApplication.launchWrapper("")).start();

            System.out.println("Starting Render Window");
            Rendering.runProgram();
        }
        else
        {
            MainApplication.addStartListener(stage ->
            {
                System.out.println("Starting Render Window");
                new Thread(() ->
                {
                    try
                    {
                        Rendering.runProgram(stage);
                    }
                    catch(InitializationException e)
                    {
                        e.printStackTrace();
                    }
                }).start();
            });

            System.out.println("Starting JavaFX UI");
            MainApplication.launchWrapper("");
        }

        System.out.println("Boot Complete");

    }
}
