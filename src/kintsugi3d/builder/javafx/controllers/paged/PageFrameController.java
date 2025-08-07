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
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PageFrameController
{
    private static final Logger log = LoggerFactory.getLogger(PageFrameController.class);
    @FXML private Pane outerRoot;

    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML private Pane hostPane;

    private final Map<String, Page<?,?>> pageCache = new HashMap<>(8);
    private Function<FXMLLoader, FXMLLoader> pageFactory;
    private Page<?,?> currentPage;

    public void init()
    {
        Window window = outerRoot.getScene().getWindow();
        Platform.runLater(window::requestFocus);
        window.setOnCloseRequest(this::onCloseRequest);

        // Force the window back to the correct size in case of race conditions with the OS (esp. on Linux)
        ChangeListener<? super Number> forceSize =
            (obs, oldValue, newValue) ->
                Platform.runLater(outerRoot.getScene().getWindow()::sizeToScene);
        window.widthProperty().addListener(forceSize);
        window.heightProperty().addListener(forceSize);

        String fileName = currentPage.getFXMLFilePath();
        initControllerAndUpdatePanel(fileName);

        setButtonShortcuts(currentPage.getController());
        prevButton.setOnAction(e -> prevPage());
        nextButton.setOnAction(e -> advancePage());
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
        Window window = currentPage.getController().getRootNode().getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void onCloseRequest(WindowEvent windowEvent)
    {
        if (!currentPage.getController().close())
        {
            windowEvent.consume();
        }

        // the welcome window doesn't open like it should
        if (!currentPage.getController().isConfirmed())
        {
            WelcomeWindowController.getInstance().showIfNoModelLoadedAndNotProcessing();
        }
    }

    public void setPageFactory(Function<FXMLLoader, FXMLLoader> pageFactory)
    {
        this.pageFactory = pageFactory;
    }

    public <PageType extends Page<InType,OutType>, InType, OutType> PageType createPage(String fxmlPath,
        Function<FXMLLoader, FXMLLoader> customPageFactory, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        PageType page = pageConstructor.apply(fxmlPath, customPageFactory.apply(loader));

        PageController<?> controller = page.getController();
        controller.setPageFrameController(this);
        nextButton.disableProperty().bind(
            page.getNextPageObservable().isNull()
                .and(controller.getCanConfirmObservable().not())
                .or(controller.getCanAdvanceObservable().not()));

        BooleanBinding advanceLabelOverrideNotNull = controller.getAdvanceLabelOverrideObservable().isNotNull();
        BooleanBinding shouldConfirm = page.getNextPageObservable().isNotNull().and(controller.getCanConfirmObservable());

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

        controller.getCanConfirmObservable().addListener(observable ->
            nextButton.setFont(Font.font(
                nextButton.getFont().getFamily(),
                controller.canConfirm() ? FontWeight.BOLD : FontWeight.NORMAL,
                nextButton.getFont().getSize())));

        page.initController();
        pageCache.put(fxmlPath, page);
        return page;
    }

    public <PageType extends Page<InType, OutType>, InType, OutType> PageType createPage(
        String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return createPage(fxmlPath, pageFactory, pageConstructor);
    }

    public Page<?,?> getPage(String fxmlPath)
    {
        return pageCache.get(fxmlPath);
    }

    public void prevPage()
    {
        if (currentPage.hasPrevPage())
        {
            currentPage = currentPage.getPrevPage();
            initControllerAndUpdatePanel(currentPage.getFXMLFilePath());
        }
        else
        {
            close();
        }
    }

    public void advancePage()
    {
        if (currentPage.getController().canAdvance())
        {
            if (currentPage.getController().advance()) // Finishes up on the current page
            {
                if (currentPage.hasNextPage()) // next page exists
                {
                    // Passes data to the next page if applicable
                    currentPage.submit();

                    currentPage = currentPage.getNextPage();
                    initControllerAndUpdatePanel(currentPage.getFXMLFilePath());
                    setButtonShortcuts(currentPage.getController());
                }
                else if (currentPage.getController().canConfirm()) // no next page but can submit
                {
                    if (currentPage.getController().getConfirmCallback() != null)
                    {
                        currentPage.getController().getConfirmCallback().run();
                    }

                    if (currentPage.getController().confirm())
                    {
                        WelcomeWindowController.getInstance().hide();

                        Window window = currentPage.getController().getRootNode().getScene().getWindow();
                        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
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

        currentPage.getController().refresh();

        outerRoot.getScene().getWindow().sizeToScene();
    }

    public Page<?,?> getCurrentPage()
    {
        return currentPage;
    }

    public void setCurrentPage(Page<?,?> page)
    {
        this.currentPage = page;
    }
}

