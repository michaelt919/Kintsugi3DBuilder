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

import javafx.fxml.FXMLLoader;

/**
 * Internal base class that is extended for the public-facing classes
 * @param <InType>
 * @param <OutType>
 */
class DataSupplierPageBase<InType, OutType, ControllerType extends DataSupplierPageController<? super InType, OutType>>
    extends PageBase<InType, OutType, ControllerType>
    implements DataSupplierPage<InType, OutType>
{
    private OutType data;

    public DataSupplierPageBase(FXMLLoader loader)
    {
        super(loader);
    }

    @Override
    public final void initController()
    {
        this.getController().setPage(this);
        this.getController().initPage();
    }

    @Override
    public final OutType getOutData()
    {
        return this.data;
    }

    @Override
    public final void setOutData(OutType data)
    {
        this.data = data;
    }
}
