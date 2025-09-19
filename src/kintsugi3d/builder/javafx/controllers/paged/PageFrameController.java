/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.paged;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.core.JavaFXState;
import kintsugi3d.builder.javafx.experience.Modal;

import java.util.function.Function;
import java.util.function.Supplier;

public class PageFrameController
{
    @FXML private Pane outerRoot;

    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML private Pane hostPane;

    private Function<FXMLLoader, FXMLLoader> pageFactory;
    private final ObjectProperty<Page<?,?>> currentPage = new SimpleObjectProperty<>();

    private Runnable confirmCallback;

    private Window window;
    private JavaFXState state;

    public Window getWindow()
    {
        return window;
    }

    public JavaFXState getState()
    {
        return state;
    }

    public void setState(JavaFXState state)
    {
        this.state = state;
    }

    private boolean isConfirmed = false;

    public void init()
    {
        this.window = outerRoot.getScene().getWindow();

        Platform.runLater(window::requestFocus);
        window.setOnCloseRequest(this::onCloseRequest);

        // Force the window back to the correct size in case of race conditions with the OS (esp. on Linux)
        ChangeListener<? super Number> forceSize =
            (obs, oldValue, newValue) ->
                Platform.runLater(outerRoot.getScene().getWindow()::sizeToScene);
        window.widthProperty().addListener(forceSize);
        window.heightProperty().addListener(forceSize);

        initControllerAndUpdatePanel();

        prevButton.textProperty().bind(new StringBinding()
        {
            {
                bind(currentPage);
            }

            @Override
            protected String computeValue()
            {
                return currentPage.getValue().getPrevPage() == null ? "Cancel" : "Back";
            }
        });

        setButtonShortcuts(currentPage.get().getController());
    }

    public PageFrameController setMinContentWidth(double width)
    {
        hostPane.setMinWidth(width);
        return this;
    }

    public PageFrameController setMinContentHeight(double height)
    {
        hostPane.setMinHeight(height);
        return this;
    }

    private void setButtonShortcuts(PageController<?> pageController)
    {
        KeyCombination leftKeyCode = new KeyCodeCombination(KeyCode.A);
        KeyCombination rightKeyCode = new KeyCodeCombination(KeyCode.D);

        Scene scene = pageController.getRootNode().getScene();
        scene.getAccelerators().put(leftKeyCode, () -> prevButton.fire());
        scene.getAccelerators().put(rightKeyCode, () -> nextButton.fire());
    }

    private void close()
    {
        Modal.requestClose(currentPage.get().getController().getRootNode());
    }

    private void onCloseRequest(WindowEvent windowEvent)
    {
        // If confirmed, do nothing.
        // Otherwise, try to close the current page, but if it's overridden, consume the event to prevent the modal from closing.
        if (!isConfirmed && !currentPage.get().getController().cancel())
        {
            windowEvent.consume();
        }
    }

    public void setPageFactory(Function<FXMLLoader, FXMLLoader> pageFactory)
    {
        this.pageFactory = pageFactory;
    }

    /**
     * Constructs a page from an FXML file
     * but overrides the controller type specified in the FXML by providing a constructor for the desired controller.
     * @param fxmlPath
     * @param pageConstructor
     * @param controllerConstructorOverride
     * @return
     * @param <PageType>
     * @param <ControllerType>
     */
    <PageType extends Page<?, ?>, ControllerType extends PageController<?>>
    PageType createPage(String fxmlPath, Function<FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        if (controllerConstructorOverride != null)
        {
            // Override controller type if specified.
            loader.setControllerFactory(c -> controllerConstructorOverride.get());
        }

        PageType page = pageConstructor.apply(pageFactory.apply(loader));

        PageController<?> controller = page.getController();
        controller.setPageFrameController(this);

        BooleanBinding advanceLabelOverrideNotNull = controller.getAdvanceLabelOverrideObservable().isNotNull();
        BooleanBinding shouldConfirm = page.getNextPageObservable().isNull().and(controller.getCanConfirmObservable());

        currentPage.addListener(obs ->
        {
            // if the current page is changed to the page just created, bind it to button attributes as needed
            if (currentPage.get() == page)
            {
                nextButton.textProperty().unbind();
                nextButton.textProperty().bind(new ObjectBinding<>()
                {
                    {
                        bind(advanceLabelOverrideNotNull);
                        bind(shouldConfirm);
                    }

                    @Override
                    protected String computeValue()
                    {
                        if (advanceLabelOverrideNotNull.get())
                        {
                            return controller.getAdvanceLabelOverrideObservable().get();
                        }
                        else
                        {
                            return shouldConfirm.get() ? "Confirm" : "Next";
                        }
                    }
                });

                nextButton.disableProperty().unbind();
                nextButton.disableProperty().bind(
                    page.getNextPageObservable().isNull()
                        .and(controller.getCanConfirmObservable().not())
                        .or(controller.getCanAdvanceObservable().not()));
            }
        });

        controller.getCanConfirmObservable().addListener(observable ->
        {
            if (currentPage == page) // only should apply if on the page just created
            {
                nextButton.setFont(Font.font(
                    nextButton.getFont().getFamily(),
                    controller.canConfirm() ? FontWeight.BOLD : FontWeight.NORMAL,
                    nextButton.getFont().getSize()));
            }
        });

        page.initController();
        return page;
    }

    public NonDataPageBuilder<PageFrameController> buildPage(Runnable callback)
    {
        return new NonDataSentinelPageBuilder(this,
            () ->
            {
                PageFrameController.this.init();
                callback.run();
                return PageFrameController.this;
            });
    }

    public <T> SimplePageBuilder<Object, T, PageFrameController> buildPage(Runnable callback, T data)
    {
        return new DataSentinelPageBuilder<>(this,
            () ->
            {
                PageFrameController.this.init();
                callback.run();
                return PageFrameController.this;
            },
            data);
    }

    public void prevPage()
    {
        if (currentPage.get().hasPrevPage())
        {
            if (currentPage.get().getController().cancel()) // Cancel the current page, could be overridden.
            {
                currentPage.set(currentPage.get().getPrevPage());
                initControllerAndUpdatePanel();
            }
        }
        else
        {
            close();
        }
    }

    public void advancePage()
    {
        if (currentPage.get().getController().canAdvance())
        {
            if (currentPage.get().getController().advance()) // Finishes up on the current page
            {
                if (currentPage.get().hasNextPage()) // next page exists
                {
                    // Passes data to the next page if applicable
                    currentPage.get().sendOutData();

                    // Set the link back from the next page to this page.
                    currentPage.get().linkBackFromNextPage();

                    // Actual advance the page.
                    currentPage.set(currentPage.get().getNextPage());

                    // Initialization
                    initControllerAndUpdatePanel();
                    setButtonShortcuts(currentPage.get().getController());
                }
                else if (currentPage.get().getController().canConfirm()) // no next page but can submit
                {
                    if (currentPage.get().getController().confirm())
                    {
                        isConfirmed = true;

                        if (confirmCallback != null)
                        {
                            confirmCallback.run();
                        }

                        Modal.requestClose(currentPage.get().getController().getRootNode());
                    }
                }
            }
        }
    }

    public void fallbackPage(String pageName)
    {
        Page<?,?> fallbackPage = currentPage.get().getFallbackPages().get(pageName);
        if (fallbackPage != null)
        {
            currentPage.set(fallbackPage);
            initControllerAndUpdatePanel();
        }
        else
        {
            close();
        }
    }

    private void initControllerAndUpdatePanel()
    {
        Parent newContent = getCurrentPage().getRoot();

        if (newContent != null)
        {
            hostPane.getChildren().setAll(newContent);
        }

        currentPage.get().getController().refresh();
        outerRoot.getScene().getWindow().sizeToScene();
    }

    public Page<?,?> getCurrentPage()
    {
        return currentPage.get();
    }

    public void setCurrentPage(Page<?, ?> page)
    {
        currentPage.set(page);
    }

    public Runnable getConfirmCallback()
    {
        return confirmCallback;
    }

    public PageFrameController setConfirmCallback(Runnable confirmCallback)
    {
        this.confirmCallback = confirmCallback;
        return this;
    }
}
