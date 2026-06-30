/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.experience;

import javafx.beans.binding.BooleanExpression;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.controllers.paged.*;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Supplier;

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

        if (state != null)
        {
            // Close modal window when project is closed.
            BooleanExpression projectLoadedProperty = state.getProjectModel().getProjectLoadedProperty();
            projectLoadedProperty.addListener(obs ->
            {
                if (!projectLoadedProperty.get())
                {
                    modal.requestClose();
                }
            });
        }
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
        PageFrameController frameController = createModal("/fxml/PageFrame.fxml");
        frameController.setState(getState());

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
     * Builds a paged modal with multiple pages.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final NonDataPageBuilder<PageFrameController> buildPagedModal() throws IOException
    {
        return createPagedModal().buildPage(modal::open);
    }

    /**
     * Builds a paged modal with multiple pages.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final <T> SimplePageBuilder<Object, T, PageFrameController> buildPagedModal(T data) throws IOException
    {
        return createPagedModal().buildPage(modal::open, data);
    }

    /**
     * Creates and opens this experience in a paged modal window using the PageController framework.
     * @param firstPageURLString The path of the FXML to be loaded for the first page.
     * @param firstPageControllerConstructorOverride Overrides the controller type specified in the FXML
     *                                       by providing a constructor for the desired controller
     * @return The PageFrameController for the window housing this experience.
     * @param <ControllerType> The type of controller to be used for the first page.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final <ControllerType extends NonSupplierPageController<Object>>
    PageFrameController openPagedModal(
        String firstPageURLString, Supplier<ControllerType> firstPageControllerConstructorOverride) throws IOException
    {
        return buildPagedModal()
            .<SimpleNonDataPage<ControllerType>, Object, ControllerType>then(
                firstPageURLString, SimpleNonDataPage::new, firstPageControllerConstructorOverride).
            finish();
    }

    /**
     * Creates and opens this experience in a paged modal window using the PageController framework.
     * @param firstPageURLString The path of the FXML to be loaded for the first page.
     * @return The PageFrameController for the window housing this experience.
     * @throws IOException If the FXML could not be loaded.
     */
    protected final <ControllerType extends NonSupplierPageController<Object>>
    PageFrameController openPagedModal(String firstPageURLString) throws IOException
    {
        return this.<ControllerType>openPagedModal(firstPageURLString, null);
    }
    /**
     * Handles errors that occur opening this experience (logging them and popping up a dialog window indicating that an error occurred).
     * @param e The exception that occurred.
     */
    protected void handleError(Exception e)
    {
        ExceptionHandling.error(MessageFormat.format("An error occurred opening window:\n{0}", getName()), e);
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
