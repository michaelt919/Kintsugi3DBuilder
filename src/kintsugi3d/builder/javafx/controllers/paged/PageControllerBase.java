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

public abstract class PageControllerBase<PageType extends Page<?>> implements PageController<PageType>
{
    protected Runnable confirmCallback;
    private PageType page;

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
    public boolean isNextButtonValid()
    {
        return page.hasNextPage();
    }

    @Override
    public boolean nextButtonPressed()
    {
        return true;
    }

    @Override
    public boolean closeButtonPressed()
    {
        return true;
    }

    public void setConfirmCallback(Runnable callback)
    {
        this.confirmCallback = callback;
    }
}
