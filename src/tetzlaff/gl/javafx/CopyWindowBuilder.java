package tetzlaff.gl.javafx;

import java.util.function.Function;

import javafx.application.Platform;
import javafx.stage.Stage;
import tetzlaff.gl.builders.framebuffer.DefaultFramebufferFactory;
import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.core.DoubleFramebufferObject;
import tetzlaff.gl.glfw.WindowContextBase;
import tetzlaff.gl.window.PollableWindow;
import tetzlaff.gl.window.WindowBuilderBase;

public class CopyWindowBuilder<ContextType extends WindowContextBase<ContextType>>
    extends WindowBuilderBase<ContextType>
{
    private final ContextType context;
    private final Stage primaryStage;
    private DoubleFramebufferObject<ContextType> framebuffer;
    private volatile PollableWindow<ContextType> result;

    public CopyWindowBuilder(Stage primaryStage,
        Function<Function<ContextType, DoubleFramebuffer<ContextType>>, ContextType> createContext,
        String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.primaryStage = primaryStage;
        this.context = createContext.apply(c ->
        {
            framebuffer = DefaultFramebufferFactory.create(c, width, height);
            return framebuffer;
        });
    }

    @Override
    public PollableWindow<ContextType> create()
    {
        Platform.runLater(() -> result = new WindowImpl<>(primaryStage, context, framebuffer, this));

        while(result == null)
        {
        }

        return result;
    }
}
