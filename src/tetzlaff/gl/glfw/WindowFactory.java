package tetzlaff.gl.glfw;

import javafx.stage.Stage;
import tetzlaff.gl.javafx.CopyWindowBuilder;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLContextFactory;

public final class WindowFactory
{
    private WindowFactory()
    {
    }

    public static WindowBuilderImpl<OpenGLContext> buildOpenGLWindow(String title, int width, int height)
    {
        return new WindowBuilderImpl<>(OpenGLContextFactory.getInstance(), title, width, height);
    }

    public static CopyWindowBuilder<OpenGLContext> buildJavaFXWindow(Stage primaryStage, String title, int width, int height)
    {
        return new CopyWindowBuilder<>(primaryStage,
            f -> new WindowBuilderImpl<>(OpenGLContextFactory.getInstance(), "<ignore>", 1, 1)
                .setDefaultFramebufferCreator(f)
                .create()
                .getContext(),
            title, width, height);
    }
}
