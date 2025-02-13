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

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import javafx.util.Pair;
import kintsugi3d.builder.core.ViewSet;

import java.util.function.Consumer;

public abstract class FXMLPageController {

    protected FXMLPageScrollerController hostScrollerController;
    protected FXMLPage hostPage;
    protected Runnable loadStartCallback;
    protected Consumer<ViewSet> viewSetCallback;

    public void setHostScrollerController(FXMLPageScrollerController scroller){this.hostScrollerController = scroller;}
    public FXMLPageScrollerController getHostScrollerController(){return hostScrollerController;}

    public void setHostPage(FXMLPage page){this.hostPage = page;}
    public FXMLPage getHostPage(){return this.hostPage;}

    public abstract Region getHostRegion(); //returns the outer anchorpane, vbox, gridpane, etc. for the controller's fxml

    public abstract void init();

    public abstract void refresh();

    public void openChildPage(String childFXMLPath) {
        hostPage.setNextPage(hostScrollerController.getPage(childFXMLPath));
        hostScrollerController.nextPage();
    }

    public Pair<Double, Double> getSizePreferences(){
        Region hostNode = getHostRegion();

        //add a bit of padding
        return new Pair<>(hostNode.getPrefWidth() * 1.02, hostNode.getPrefHeight() * 1.02);
    }

    public boolean isNextButtonValid(){return hostPage.hasNextPage();}

    public void setButtonShortcuts(){
        KeyCombination leftKeyCode = new KeyCodeCombination(KeyCode.A);

        KeyCombination rightKeyCode = new KeyCodeCombination(KeyCode.D);

        Scene scene = getHostRegion().getScene();
        scene.getAccelerators().put(leftKeyCode, ()-> hostScrollerController.getPrevButton().fire());
        scene.getAccelerators().put(rightKeyCode, ()-> hostScrollerController.getNextButton().fire());
    }

    public void setLoadStartCallback(Runnable callback){this.loadStartCallback = callback;}
    public void setViewSetCallback(Consumer<ViewSet> callback){this.viewSetCallback = callback;}

    public boolean nextButtonPressed()
    {
        return true;
    }

    public boolean closeButtonPressed()
    {
        return true;
    }
}
