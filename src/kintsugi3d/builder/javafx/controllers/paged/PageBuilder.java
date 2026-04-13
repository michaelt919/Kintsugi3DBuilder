/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public abstract class PageBuilder<FinishType>
{
    protected static final String SELECTION_PAGE_FXML = "/fxml/SelectionPage.fxml";

    protected final PageFrameController frameController;
    protected final Supplier<FinishType> finisher;

    public PageBuilder(PageFrameController frameController, Supplier<FinishType> finisher)
    {
        this.frameController = frameController;
        this.finisher = finisher;
    }

    public FinishType finish()
    {
        return finisher.get();
    }

    public abstract Page<?,?> getPage();
}
