package kintsugi3d.builder.javafx.experience;

import javafx.fxml.FXMLLoader;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.Modal;
import kintsugi3d.builder.javafx.controllers.paged.Page;
import kintsugi3d.builder.javafx.controllers.paged.PageFrameController;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.BiFunction;

/**
 * A base class for experiences that provides most of the boilerplate.
 * Subclasses just need to provide implementations of getName() and open().
 */
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

    @Override
    public final boolean isOpen()
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

    /**
     * Opens the experience, typically in a modal window.
     * @throws IOException if the FXML could not be loaded.
     */
    protected abstract void open() throws IOException;

    /**
     * Creates a modal to house this experience using the URL string of the FXML.
     * @param urlString The path of the FXML to be loaded.
     * @return The controller for the new modal window.
     * @param <ControllerType> The type of the controller.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final <ControllerType> ControllerType createModal(String urlString) throws IOException
    {
        return modal.create(getName(), urlString);
    }

    /**
     * Creates and opens this experience in a modal window using the URL string of the FXML.
     * @param urlString The path of the FXML to be loaded.
     * @return The controller for the new modal window.
     * @param <ControllerType> The type of the controller.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final <ControllerType> ControllerType openModal(String urlString) throws IOException
    {
        ControllerType controller = createModal(urlString);
        modal.open();
        return controller;
    }

    /**
     * Creates a paged modal window to house this experience using the PageController framework.
     * @return The PageFrameController for the window housing this experience.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final PageFrameController createPagedModal() throws IOException
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

    /**
     * Creates and opens this experience in a paged modal window using the PageController framework.
     * @param firstPageURLString The path of the FXML to be loaded for the first page.
     * @param firstPageConstructor The constructor or a method effectively constructing the first page object.
     *                             Typically this will be in the form of SomeSubclassOfPage::new.
     * @return The PageFrameController for the window housing this experience.
     * @param <PageType> The type of the first page.
     * @param <InType> The type of page that the first page can link to as a previous page.
     * @param <OutType> The type of page that the first page can link to as a next page.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final <PageType extends Page<InType, OutType>, InType, OutType>
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

    /**
     * Handles errors that occur opening this experience (logging them and popping up a dialog window indicating that an error occurred).
     * @param e The exception that occurred.
     */
    protected void handleError(Exception e)
    {
        ExceptionHandling.error(MessageFormat.format("An error occurred opening window: {0}", getName()), e);
    }

    @Override
    public boolean isInitialized()
    {
        return parentWindow != null;
    }

    @Override
    public Window getParentWindow()
    {
        return parentWindow;
    }

    @Override
    public Modal getModal()
    {
        return modal;
    }

    /**
     * Gets the state-related data that the experience has access to
     * @return The state object.
     */
    protected JavaFXState getState()
    {
        return state;
    }
}
