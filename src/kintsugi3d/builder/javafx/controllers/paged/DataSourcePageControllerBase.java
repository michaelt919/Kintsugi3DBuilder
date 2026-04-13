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

/**
 * Base class for controllers that require a page that can store data for supplying to following pages
 * and do not receive any data from previous pages (any data received will be discarded).
 * May be attached to a SimpleDataSourcePage with matching T or a SimpleDataTransformerPage with OutType matching T.
 * @param <T>
 */
public abstract class DataSourcePageControllerBase<T>
    extends PageControllerBase<Object, DataSupplierPage<?, T>>
    implements DataSupplierPageController<Object, T>
{
    @Override
    public final void receiveData(Object data)
    {
    }
}
