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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;

abstract class PageBase<InType, OutType, ControllerType extends PageController<? super InType>>
    implements Page<InType, OutType>
{
    private final String fxmlFilePath;

    private final ControllerType controller;

    private final FXMLLoader loader;

    private final ObjectProperty<Page<? super OutType, ?>> nextPageProperty = new SimpleObjectProperty<>(null);

    private Page<?, ? extends InType> prevPage = null;

    protected PageBase(String fxmlFile, FXMLLoader loader)
    {
        this.fxmlFilePath = fxmlFile;
        this.loader = loader;
        this.controller = loader == null ? null : loader.getController();
    }

    @Override
    public final ControllerType getController()
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
    public final ObjectProperty<Page<? super OutType, ?>> getNextPageObservable()
    {
        return nextPageProperty;
    }

    @Override
    public final Page<? super OutType, ?> getNextPage()
    {
        return nextPageProperty.get();
    }

    @Override
    public final boolean hasNextPage()
    {
        return nextPageProperty.isNotNull().get();
    }

    @Override
    public void setNextPage(Page<? super OutType, ?> page)
    {
        nextPageProperty.set(page);

        if (page != null)
        {
            page.setPrevPage(this);
        }
    }

    @Override
    public final Page<?, ? extends InType> getPrevPage()
    {
        return prevPage;
    }

    @Override
    public final boolean hasPrevPage()
    {
        return prevPage != null;
    }

    @Override
    public void setPrevPage(Page<?, ? extends InType> page)
    {
        this.prevPage = page;
    }

    @Override
    public void receiveData(InType data)
    {
        this.controller.receiveData(data);
    }

    @Override
    public final void sendOutData()
    {
        if (this.hasNextPage())
        {
            this.getNextPage().receiveData(this.getOutData());
        }
    }
}
