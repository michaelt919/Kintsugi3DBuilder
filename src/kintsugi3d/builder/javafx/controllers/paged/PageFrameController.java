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
import kintsugi3d.builder.javafx.Modal;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class PageFrameController
{
    @FXML private Pane outerRoot;

    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML private Pane hostPane;

    private final Map<String, Page<?,?>> pageCache = new HashMap<>(8);
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

    public void init(JavaFXState state)
    {
        this.window = outerRoot.getScene().getWindow();
        this.state = state;

        Platform.runLater(window::requestFocus);
        window.setOnCloseRequest(this::onCloseRequest);

        // Force the window back to the correct size in case of race conditions with the OS (esp. on Linux)
        ChangeListener<? super Number> forceSize =
            (obs, oldValue, newValue) ->
                Platform.runLater(outerRoot.getScene().getWindow()::sizeToScene);
        window.widthProperty().addListener(forceSize);
        window.heightProperty().addListener(forceSize);

        String fileName = currentPage.get().getFXMLFilePath();
        initControllerAndUpdatePanel(fileName);

        setButtonShortcuts(currentPage.get().getController());
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
        if (!currentPage.get().getController().close())
        {
            windowEvent.consume();
        }
    }

    public void setPageFactory(Function<FXMLLoader, FXMLLoader> pageFactory)
    {
        this.pageFactory = pageFactory;
    }

    /**
     * Constructs a page from an FXML file.
     * @param fxmlPath
     * @param pageConstructor
     * @return
     * @param <PageType>
     */
    public <PageType extends Page<?, ?>>
    PageType createPage(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return createPage(fxmlPath, pageConstructor, null);
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
    public <PageType extends Page<?, ?>, ControllerType extends PageController<?>>
    PageType createPage(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        if (controllerConstructorOverride != null)
        {
            // Override controller type if specified.
            loader.setControllerFactory(c -> controllerConstructorOverride.get());
        }

        PageType page = pageConstructor.apply(fxmlPath, pageFactory.apply(loader));

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
        pageCache.put(fxmlPath, page);
        return page;
    }

    public <InType, OutType, PageType extends Page<InType, OutType>, ControllerType extends PageController<?>>
    DataPageBuilder<InType, OutType, PageFrameController> begin(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor,
        JavaFXState state, Runnable callback, Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType page = createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.currentPage.set(page);
        return new DataPageBuilder<>(page, this,
            () ->
            {
                PageFrameController.this.init(state);
                callback.run();
                return PageFrameController.this;
            });
    }

    public Page<?,?> getPage(String fxmlPath)
    {
        return pageCache.get(fxmlPath);
    }

    public void prevPage()
    {
        if (currentPage.get().hasPrevPage())
        {
            currentPage.set(currentPage.get().getPrevPage());
            initControllerAndUpdatePanel(currentPage.get().getFXMLFilePath());
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

                    currentPage.set(currentPage.get().getNextPage());
                    initControllerAndUpdatePanel(currentPage.get().getFXMLFilePath());
                    setButtonShortcuts(currentPage.get().getController());
                }
                else if (currentPage.get().getController().canConfirm()) // no next page but can submit
                {
                    if (currentPage.get().getController().confirm())
                    {
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

    private void initControllerAndUpdatePanel(String fileName)
    {
        Page<?,?> newPage = getPage(fileName);
        Parent newContent = newPage.getLoader().getRoot();

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

    public Runnable getConfirmCallback()
    {
        return confirmCallback;
    }

    public void setConfirmCallback(Runnable confirmCallback)
    {
        this.confirmCallback = confirmCallback;
    }
}

