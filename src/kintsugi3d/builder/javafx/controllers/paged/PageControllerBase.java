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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public abstract class PageControllerBase<PageType extends Page<?>> implements PageController<PageType>
{
    private PageFrameController pageFrameController;
    private PageType page;
    private Runnable confirmCallback;
    private final BooleanProperty canAdvanceProperty = new SimpleBooleanProperty(false);
    private final StringProperty advanceLabelOverrideProperty = new SimpleStringProperty(null);
    private final BooleanProperty canConfirmProperty = new SimpleBooleanProperty(false);

    private boolean isConfirmed;

    @Override
    public PageFrameController getPageFrameController()
    {
        return pageFrameController;
    }

    @Override
    public void setPageFrameController(PageFrameController scroller)
    {
        this.pageFrameController = scroller;
    }

    @Override
    public PageType getPage()
    {
        return this.page;
    }

    @Override
    public void setPage(PageType page)
    {
        this.page = page;
    }

    @Override
    public final boolean canAdvance()
    {
        return getCanAdvanceObservable().get();
    }

    @Override
    public final BooleanProperty getCanAdvanceObservable()
    {
        return canAdvanceProperty;
    }

    @Override
    public final StringProperty getAdvanceLabelOverrideObservable()
    {
        return advanceLabelOverrideProperty;
    }

    @Override
    public final String getAdvanceLabelOverride()
    {
        return getAdvanceLabelOverrideObservable().get();
    }

    @Override
    public final BooleanProperty getCanConfirmObservable()
    {
        return canConfirmProperty;
    }

    @Override
    public final boolean canConfirm()
    {
        return canConfirmProperty.get();
    }

    @Override
    public boolean advance()
    {
        return true;
    }

    @Override
    public boolean close()
    {
        return true;
    }

    @Override
    public Runnable getConfirmCallback()
    {
        return confirmCallback;
    }

    @Override
    public void setConfirmCallback(Runnable callback)
    {
        this.confirmCallback = callback;
    }

    @Override
    public boolean isConfirmed()
    {
        return isConfirmed;
    }

    public void setConfirmed(boolean confirmed)
    {
        this.isConfirmed = confirmed;
    }
}
