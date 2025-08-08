package kintsugi3d.builder.javafx.experience;

import javafx.stage.Window;
import kintsugi3d.builder.javafx.JavaFXState;
import kintsugi3d.builder.javafx.controllers.paged.PageFrameController;
import kintsugi3d.builder.javafx.util.ExceptionHandling;
import kintsugi3d.builder.javafx.window.ModalWindow;

import java.io.IOException;
import java.text.MessageFormat;

public abstract class ExperienceBase implements Experience
{
    private ModalWindow modal;
    private JavaFXState state;

    @Override
    public final void initialize(Window parentWindow, JavaFXState state)
    {
        this.modal = new ModalWindow(parentWindow);
        this.state = state;
    }

    public void tryOpen()
    {
        if (modal.isOpen())
        {
            try
            {
                open();
            }
            catch (Exception e)
            {
                handleError(e);
            }
        }
    }

    protected <ControllerType> ControllerType openModal(String urlString) throws IOException
    {
        ControllerType controller = createModal(urlString);
        modal.open();
        return controller;
    }

    protected <ControllerType> ControllerType createModal(String urlString) throws IOException
    {
        return modal.create(getName(), urlString);
    }

    protected PageFrameController openPagedModal() throws IOException
    {
        PageFrameController frameController = openModal("fxml/PageFrame.fxml");

        frameController.setPageFactory(loader ->
        {
            try
            {
                loader.load();
            }
            catch (Exception e)
            {
                handleError(e);
            }

            return loader;
        });

        return frameController;
    }

    protected void handleError(Exception e)
    {
        ExceptionHandling.error(MessageFormat.format("An error occurred opening window: {0}", getName()), e);
    }

    protected ModalWindow getModal()
    {
        return modal;
    }

    protected JavaFXState getState()
    {
        return state;
    }
}
