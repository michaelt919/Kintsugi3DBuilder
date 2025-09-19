package kintsugi3d.builder.javafx.experience;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.javafx.core.MainWindowController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Creates a modal window.
 * Only one modal window managed by a single instance of this class can be open at a time.
 */
public class Modal
{
    private final Window parentWindow;
    private Stage stage;

    private final BooleanProperty openProperty = new SimpleBooleanProperty(false);

    public Modal(Window parentWindow)
    {
        this.parentWindow = parentWindow;
    }

    public BooleanExpression getOpenProperty()
    {
        return openProperty;
    }

    public boolean isOpen()
    {
        return openProperty.get();
    }

    public Stage getStage()
    {
        return stage;
    }

    /**
     * Opens a modal window and returns the associated controller.
     * Returns null if window is already open or if an error occurred.
     * @param title
     * @param urlString
     * @return
     * @param <ControllerType>
     * @throws IOException
     */
    public <ControllerType> ControllerType create(String title, String urlString) throws IOException
    {
        return create(title, getFXMLLoader(urlString));
    }

    public <ControllerType> ControllerType create(String title, FXMLLoader fxmlLoader) throws IOException
    {
        Parent root = fxmlLoader.load();

        this.stage = new Stage();
        stage.getIcons().add(MainApplication.getIcon());
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.initOwner(parentWindow);
        stage.setResizable(false);
        openProperty.bind(stage.showingProperty());
        MainApplication.initAccelerators(stage.getScene());

        return fxmlLoader.getController();
    }

    public void open()
    {
        stage.show();
    }

    public void requestClose()
    {
        requestClose(stage);
    }

    public static void requestClose(Node node)
    {
        requestClose(node.getScene().getWindow());
    }

    public static void requestClose(Window window)
    {
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private static FXMLLoader getFXMLLoader(String urlString) throws FileNotFoundException
    {
        URL url = MainWindowController.class.getResource(urlString);
        if (url == null)
        {
            throw new FileNotFoundException(urlString);
        }
        return new FXMLLoader(url);
    }
}
