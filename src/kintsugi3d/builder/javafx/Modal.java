package kintsugi3d.builder.javafx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.util.Flag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Creates a modal window.
 * Only one modal window managed by a single instance of this class can be open at a time.
 */
public class Modal
{
    private static final String ICON_PATH = "Kintsugi3D-icon.png";

    private final Window parentWindow;
    private final Flag open = new Flag(false);


    private Stage stage;

    public Modal(Window parentWindow)
    {
        this.parentWindow = parentWindow;
    }

    public boolean isOpen()
    {
        return open.get();
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
        stage.getIcons().add(new Image(new File(ICON_PATH).toURI().toURL().toString()));
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.initOwner(parentWindow);
        stage.setResizable(false);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, x ->
        {
            open.set(false);
            WelcomeWindowController.getInstance().showIfNoModelLoadedAndNotProcessing();
        });

        return fxmlLoader.getController();
    }

    public void open()
    {
        open.set(true);
        stage.show();
        WelcomeWindowController.getInstance().hide();
    }

    private static FXMLLoader getFXMLLoader(String urlString) throws FileNotFoundException
    {
        URL url = MenubarController.class.getClassLoader().getResource(urlString);
        if (url == null)
        {
            throw new FileNotFoundException(urlString);
        }
        return new FXMLLoader(url);
    }
}
