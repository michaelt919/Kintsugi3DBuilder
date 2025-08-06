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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
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
    @FXML private GridPane outerGridPane;

    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML private Pane hostPane;

    private final Map<String, Page<?>> pageCache = new HashMap<>(8);
    private Function<FXMLLoader, FXMLLoader> pageFactory;
    private Page<?> currentPage;

    public void init()
    {
        String fileName = currentPage.getFXMLFilePath();
        initControllerAndUpdatePanel(fileName);

        setButtonShortcuts(currentPage.getController());
        Platform.runLater(() -> outerGridPane.getScene().getWindow().requestFocus());

        outerGridPane.getScene().getWindow().setOnCloseRequest(this::onCloseRequest);
    }

    public void setButtonShortcuts(PageController<?> pageController)
    {
        KeyCombination leftKeyCode = new KeyCodeCombination(KeyCode.A);
        KeyCombination rightKeyCode = new KeyCodeCombination(KeyCode.D);

        Scene scene = pageController.getRootNode().getScene();
        scene.getAccelerators().put(leftKeyCode, () -> this.getPrevButton().fire());
        scene.getAccelerators().put(rightKeyCode, () -> this.getNextButton().fire());
    }

    private void close(ActionEvent actionEvent)
    {
        Window window = currentPage.getController().getRootNode().getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void onCloseRequest(WindowEvent windowEvent)
    {
        if (!currentPage.getController().closeButtonPressed())
        {
            windowEvent.consume();
        }

        // TODO: not a perfect solution. If we press the X to close the last page of a scrolling modal,
        // the welcome window doesn't open like it should
        if (!(currentPage.getController() instanceof Confirmable))
        {
            WelcomeWindowController.getInstance().showIfNoModelLoadedAndNotProcessing();
        }
    }

    public void setPageFactory(Function<FXMLLoader, FXMLLoader> pageFactory)
    {
        this.pageFactory = pageFactory;
    }

    public <PageType extends Page<?>> PageType createPage(String fxmlPath,
        Function<FXMLLoader, FXMLLoader> customPageFactory, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        PageType page = pageConstructor.apply(fxmlPath, customPageFactory.apply(loader));
        page.getController().setPageFrameController(this);
        page.initController();
        pageCache.put(fxmlPath, page);
        return page;
    }

    public <PageType extends Page<?>> PageType createPage(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return createPage(fxmlPath, pageFactory, pageConstructor);
    }

    public Page<?> getPage(String fxmlPath)
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
    }

    private void prevPage(ActionEvent e)
    {
        //use this method for setOnAction() lamdas
        prevPage();
    }

    public void nextPage()
    {
        if (currentPage.hasNextPage() && currentPage.getController().nextButtonPressed())
        {
            // Finalizes data on the page
            currentPage.getController().finish();

            // Passes data to the next page if applicable
            currentPage.submit();

            currentPage = currentPage.getNextPage();
            initControllerAndUpdatePanel(currentPage.getFXMLFilePath());
            setButtonShortcuts(currentPage.getController());
        }
    }

    private void initControllerAndUpdatePanel(String fileName)
    {
        Page<?> newPage = getPage(fileName);
        Parent newContent = newPage.getLoader().getRoot();

        if (newContent != null)
        {
            hostPane.getChildren().setAll(newContent);
        }

        outerGridPane.getScene().getWindow().sizeToScene();

        currentPage.getController().refresh();

        updatePrevAndNextButtons();
    }

    public void updatePrevAndNextButtons()
    {
        if (currentPage.hasPrevPage())
        {
            prevButton.setOnAction(this::prevPage);
        }
        else
        {
            prevButton.setOnAction(this::close);
        }
        nextButton.setDisable(!currentPage.getController().isNextButtonValid());

        //change next button to confirm button if applicable
        PageController<?> controller = currentPage.getController();

        if (controller instanceof Confirmable && ((Confirmable) controller).canConfirm())
        {
            nextButton.setText("Confirm");
            nextButton.setFont(Font.font(nextButton.getFont().getFamily(), FontWeight.BOLD, nextButton.getFont().getSize()));

            Confirmable confirmerController = (Confirmable) controller;
            nextButton.setOnAction(event -> confirmerController.confirm());
        }
        else
        {
            nextButton.setText("Next");
            nextButton.setFont(Font.font(nextButton.getFont().getFamily(), FontWeight.NORMAL, nextButton.getFont().getSize()));
            nextButton.setOnAction(event -> nextPage());
        }
    }

    public Button getNextButton()
    {
        return nextButton;
    }

    public Button getPrevButton()
    {
        return prevButton;
    }

    public void setNextButtonDisable(boolean b)
    {
        nextButton.setDisable(b);
    }

    public void updateNextButtonLabel(String labelText)
    {
        nextButton.setText(labelText);
    }

    public Page<?> getCurrentPage()
    {
        return currentPage;
    }

    public void setCurrentPage(Page<?> page)
    {
        this.currentPage = page;
    }
}

