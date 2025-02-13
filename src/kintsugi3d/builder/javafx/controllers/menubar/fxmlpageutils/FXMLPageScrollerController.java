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

package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class FXMLPageScrollerController {
    private static final Logger log = LoggerFactory.getLogger(FXMLPageScrollerController.class);
    @FXML private GridPane outerGridPane;

    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML private AnchorPane hostAnchorPane;
    ArrayList<FXMLPage> pages;
    FXMLPage currentPage;

    private final HashMap<ShareInfo.Info, Object> sharedInfo = new HashMap<>();

    public void init() {
        for (FXMLPage page : pages){
            FXMLPageController controller = page.getController();

            controller.setHostScrollerController(this);
            controller.setHostPage(page);
            controller.init();
        }

        String fileName = currentPage.getFxmlFilePath();
        initControllerAndUpdatePanel(fileName);

        currentPage.getController().setButtonShortcuts();
        Platform.runLater(()-> outerGridPane.getScene().getWindow().requestFocus());

        outerGridPane.getScene().getWindow().setOnCloseRequest(this::onCloseRequest);
    }

    private void onCloseRequest(WindowEvent windowEvent)
    {
        if (!currentPage.getController().closeButtonPressed())
        {
            windowEvent.consume();
        }
    }

    public void prevPage() {
        if (currentPage.hasPrevPage()){
            currentPage = currentPage.getPrevPage();
            initControllerAndUpdatePanel(currentPage.getFxmlFilePath());
        }
    }

    private void prevPage(ActionEvent e){
        //use this method for setOnAction() lamdas
        prevPage();
    }

    public void nextPage() {
        if (!currentPage.hasNextPage()){return;}

        if (!currentPage.getController().nextButtonPressed())
        {
            return;
        }

        String nextPath;
        try{
            nextPath = currentPage.getNextPage().getFxmlFilePath();
        }
        catch(NullPointerException e){
            log.error("Failed to load next page", e);
            return;
        }

        //send relevant info to shareInfo collection variable
        if (currentPage.getController() instanceof ShareInfo) {
            ShareInfo shareableController = (ShareInfo) currentPage.getController();
            shareableController.shareInfo();
        }

        currentPage.getNextPage().setPrevPage(currentPage);
        currentPage = currentPage.getNextPage();

        initControllerAndUpdatePanel(nextPath);
        currentPage.getController().setButtonShortcuts();
    }


    public void setPages(ArrayList<FXMLPage> pages, String firstPageFXMLPath){
        this.pages = pages;
        currentPage = getPage(firstPageFXMLPath);
    }

    public FXMLPage getPage(String fxmlPath) {
        for (FXMLPage page: pages){
            if (page.getFxmlFilePath().equals(fxmlPath)){
                return page;
            }
        }
        return null;
    }

    private void initControllerAndUpdatePanel(String fileName) {
        FXMLPage newPage = getPage(fileName);
        Parent newContent = newPage.getLoader().getRoot();

        updateSizePreferences();

        if (newContent != null) {
            hostAnchorPane.getChildren().setAll(newContent);
        }
        currentPage.getController().refresh();
        updatePrevAndNextButtons();
    }

    private void updateSizePreferences() {
        Pair<Double, Double> prefs = currentPage.getController().getSizePreferences();

        hostAnchorPane.setMinSize(prefs.getKey(), prefs.getValue());
        hostAnchorPane.setPrefSize(prefs.getKey(), prefs.getValue());

        final double EXTRA_HEIGHT = 1.2; // allow some extra breathing room for the prev and next buttons
        outerGridPane.setMinSize(prefs.getKey(), prefs.getValue() * EXTRA_HEIGHT);
        outerGridPane.setPrefSize(prefs.getKey(), prefs.getValue() * EXTRA_HEIGHT);

        Stage stage = (Stage) outerGridPane.getScene().getWindow();

        stage.setWidth(prefs.getKey() + stage.getScene().getWidth() - outerGridPane.getWidth());
        stage.setHeight((prefs.getValue() * EXTRA_HEIGHT) + stage.getScene().getHeight() - outerGridPane.getHeight());
    }


    public void updatePrevAndNextButtons() {
        //prevButton.setDisable(!currentPage.hasPrevPage());
        //instead of disabling prevButton, have it close the window instead

        if(currentPage.hasPrevPage()){
            prevButton.setOnAction(this::prevPage);
        }
        else{
            prevButton.setOnAction(this::close);
        }
        nextButton.setDisable(!currentPage.getController().isNextButtonValid());

        //change next button to confirm button if applicable
        FXMLPageController controller = currentPage.getController();

        if (controller instanceof ConfirmablePage && ((ConfirmablePage) controller).canConfirm())
        {
            nextButton.setText("Confirm");
            nextButton.setFont(Font.font(nextButton.getFont().getFamily(), FontWeight.BOLD, nextButton.getFont().getSize()));

            ConfirmablePage confirmerController = (ConfirmablePage) controller;
            nextButton.setOnAction(event->confirmerController.confirmButtonPress());
        }
        else{
            nextButton.setText("Next");
            nextButton.setFont(Font.font(nextButton.getFont().getFamily(), FontWeight.NORMAL, nextButton.getFont().getSize()));
            nextButton.setOnAction(event->nextPage());
        }
    }

    private void close(ActionEvent actionEvent) {
        Window window = currentPage.getController().getHostRegion().getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
        WelcomeWindowController.getInstance().show();
    }

    public void setNextButtonDisable(boolean b) {
        nextButton.setDisable(b);
    }

    public <T> void addInfo(ShareInfo.Info key, T info){
        sharedInfo.put(key, info);
    }

    public <T> T getInfo(ShareInfo.Info key){
        return (T) sharedInfo.get(key);
    }

    public FXMLPage getCurrentPage(){return currentPage;}

    public Button getNextButton(){return nextButton;}
    public Button getPrevButton(){return prevButton;}

    public void updateNextButtonLabel(String labelText)
    {
        nextButton.setText(labelText);
    }
}

