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
import javafx.scene.Parent;

import java.util.LinkedHashMap;
import java.util.Map;

abstract class PageBase<InType, OutType, ControllerType extends PageController<? super InType>>
    implements Page<InType, OutType>
{
    private final ControllerType controller;

    private final Parent root;

    private final ObjectProperty<Page<? super OutType, ?>> nextPageProperty = new SimpleObjectProperty<>(null);

    private Page<?, ? extends InType> prevPage = null;

    private final Map<String, Page<? super OutType,?>> fallbackPages = new LinkedHashMap<>();

    protected PageBase(FXMLLoader loader)
    {
        if (loader == null)
        {
            this.controller = null;
            this.root = null;
        }
        else
        {
            this.controller = loader.getController();
            this.root = loader.getRoot();
        }
    }

    @Override
    public final ControllerType getController()
    {
        return controller;
    }

    @Override
    public final Parent getRoot()
    {
        return root;
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

        if (page.getPrevPage() == null)
        {
            // set default link back (useful for fallback pages which weren't accessed via next button)
            page.setPrevPage(this);
        }
    }

    @Override
    public Map<String, Page<? super OutType, ?>> getFallbackPages()
    {
        return fallbackPages;
    }

    @Override
    public boolean hasFallbackPage()
    {
        return !fallbackPages.isEmpty();
    }

    @Override
    public void addFallbackPage(String fallbackName, Page<? super OutType, ?> page)
    {
        fallbackPages.put(fallbackName, page);
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
    public void linkBackFromNextPage()
    {
        this.nextPageProperty.get().setPrevPage(this);
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
