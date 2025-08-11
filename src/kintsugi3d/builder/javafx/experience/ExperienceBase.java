package kintsugi3d.builder.javafx.experience;

import javafx.fxml.FXMLLoader;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.JavaFXState;
import kintsugi3d.builder.javafx.Modal;
import kintsugi3d.builder.javafx.controllers.paged.Page;
import kintsugi3d.builder.javafx.controllers.paged.PageFrameController;
import kintsugi3d.builder.javafx.util.ExceptionHandling;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.BiFunction;

public abstract class ExperienceBase implements Experience
{
    private Window parentWindow;
    private Modal modal;
    private JavaFXState state;

    @Override
    public final void initialize(Window parentWindow, JavaFXState state)
    {
        this.parentWindow = parentWindow;
        this.modal = new Modal(parentWindow);
        this.state = state;
    }

    public boolean isOpen()
    {
        return this.modal.isOpen();
    }

    @Override
    public final void tryOpen()
    {
        if (!modal.isOpen())
        {
            try
            {
                open();
            }
            catch (IOException|RuntimeException e)
            {
                handleError(e);
            }
        }
    }

    protected <ControllerType> ControllerType createModal(String urlString) throws IOException
    {
        return modal.create(getName(), urlString);
    }

    protected <ControllerType> ControllerType openModal(String urlString) throws IOException
    {
        ControllerType controller = createModal(urlString);
        modal.open();
        return controller;
    }

    protected PageFrameController createPagedModal() throws IOException
    {
        PageFrameController frameController = createModal("fxml/PageFrame.fxml");

        frameController.setPageFactory(loader ->
        {
            try
            {
                loader.load();
            }
            catch (IOException|RuntimeException e)
            {
                handleError(e);
            }

            return loader;
        });

        return frameController;
    }

    protected <PageType extends Page<InType, OutType>, InType, OutType>
    PageFrameController openPagedModel(
        String firstPageURLString, BiFunction<String, FXMLLoader, PageType> firstPageConstructor) throws IOException
    {
        PageFrameController frameController = createPagedModal();

        Page<?,?> firstPage = frameController.createPage(firstPageURLString, firstPageConstructor);
        frameController.setCurrentPage(firstPage);
        frameController.init();

        modal.open();

        return frameController;
    }

    protected void handleError(Exception e)
    {
        ExceptionHandling.error(MessageFormat.format("An error occurred opening window: {0}", getName()), e);
    }

    protected Window getParentWindow()
    {
        return parentWindow;
    }

    protected Modal getModal()
    {
        return modal;
    }

    protected JavaFXState getState()
    {
        return state;
    }
}
