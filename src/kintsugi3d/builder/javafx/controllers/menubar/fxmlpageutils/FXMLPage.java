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

import javafx.fxml.FXMLLoader;

public class FXMLPage {
    private String fxmlFilePath;

    private FXMLPageController controller;

    private FXMLLoader loader;

    private FXMLPage prev;
    private FXMLPage next;

    public FXMLPage(String fxmlFile, FXMLLoader loader) {
        this.fxmlFilePath = fxmlFile;
        this.loader = loader;
        this.controller = loader.getController();
    }

    public FXMLPageController getController() {return controller;}
    public FXMLLoader getLoader() {return loader;}

    public String getFxmlFilePath(){
        return fxmlFilePath;
    }

    public FXMLPage getPrevPage(){return prev;}
    public FXMLPage getNextPage(){return next;}

    public boolean hasNextPage(){return next != null;}
    public boolean hasPrevPage(){return prev != null;}

    public void setPrevPage(FXMLPage page) {prev = page;}

    public void setNextPage(FXMLPage page) {next = page;}
}
