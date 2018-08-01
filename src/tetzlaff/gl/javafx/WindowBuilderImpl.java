package tetzlaff.gl.javafx;

import javafx.stage.Stage;
import tetzlaff.gl.glfw.GLFWWindowContextBase;
import tetzlaff.gl.window.PollableWindow;
import tetzlaff.gl.window.WindowBuilderBase;

class WindowBuilderImpl<ContextType extends GLFWWindowContextBase<ContextType>>
    extends WindowBuilderBase<ContextType>
{
    private final ContextType context;
    private final Stage primaryStage;

    WindowBuilderImpl(Stage primaryStage, ContextType context, String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.primaryStage = primaryStage;
        this.context = context;
    }

    @Override
    public PollableWindow<ContextType> create()
    {
        return new WindowImpl<>(primaryStage, context, this);
    }
}
