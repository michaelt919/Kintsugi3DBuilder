package kintsugi3d.builder.javafx.experience;

import javafx.stage.Window;
import kintsugi3d.builder.javafx.JavaFXState;

import java.io.IOException;

public interface Experience
{
    String getName();
    void initialize(Window parentWindow, JavaFXState state);
    void open() throws IOException;
    void tryOpen();
    boolean isInitialized();
    Window getParentWindow();
}
