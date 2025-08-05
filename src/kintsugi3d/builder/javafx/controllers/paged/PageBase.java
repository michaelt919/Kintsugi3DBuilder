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

import javafx.fxml.FXMLLoader;

abstract class PageBase<
        PrevPageType extends Page<?>,
        NextPageType extends Page<?>,
        ControllerType extends PageController<?>>
    implements Page<ControllerType>
{
    private final String fxmlFilePath;

    private final ControllerType controller;

    private final FXMLLoader loader;

    private NextPageType next;

    protected PageBase(String fxmlFile, FXMLLoader loader)
    {
        this.fxmlFilePath = fxmlFile;
        this.loader = loader;
        this.controller = loader.getController();
    }

    static <PageType extends Page<ControllerType>, ControllerType extends PageController<PageType>> void initController(PageType page)
    {
        page.getController().setPage(page);
        page.getController().init();
    }

    @Override
    public ControllerType getController()
    {
        return controller;
    }

    @Override
    public final FXMLLoader getLoader()
    {
        return loader;
    }

    @Override
    public final String getFXMLFilePath()
    {
        return fxmlFilePath;
    }

    @Override
    public final NextPageType getNextPage()
    {
        return next;
    }

    @Override
    public final boolean hasNextPage()
    {
        return next != null;
    }

    public void setNextPage(NextPageType page)
    {
        this.next = page;
    }

    @Override
    public PrevPageType getPrevPage()
    {
        return null;
    }

    @Override
    public boolean hasPrevPage()
    {
        return false;
    }
}
