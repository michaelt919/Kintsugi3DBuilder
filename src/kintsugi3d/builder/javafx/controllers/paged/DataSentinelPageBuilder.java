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

public class DataSentinelPageBuilder<T> extends DataPageBuilder<Object, T, PageFrameController>
{
    DataSentinelPageBuilder(PageFrameController frameController, Supplier<PageFrameController> finisher, T data)
    {
        super(new SentinelPage<>(), frameController, finisher);

        // Whenever the next page (i.e. true first page) is assigned, make it the current page of the frame controller.
        getPage().getNextPageObservable().addListener(
            obs ->
            {
                getPage().getNextPage().receiveData(data);
                frameController.setCurrentPage(getPage().getNextPage());
            });
    }
}
