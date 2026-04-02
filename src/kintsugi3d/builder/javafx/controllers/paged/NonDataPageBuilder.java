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

public class NonDataPageBuilder<FinishType> extends SimplePageBuilder<Object, Object, FinishType>
{
    NonDataPageBuilder(Page<Object, Object> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(page, frameController, finisher);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<FinishType> then(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return super.thenNonData(fxmlPath, controllerConstructorOverride);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<FinishType> then(String fxmlPath)
    {
        return super.<ControllerType>thenNonData(fxmlPath);
    }

    @Override
    public <ControllerType extends SelectionPageController<Object>>
    NonDataSelectionPageBuilder<FinishType> thenSelect(String prompt, Supplier<ControllerType> controllerConstructorOverride)
    {
        return thenSelectNonData(prompt, controllerConstructorOverride);
    }

    @Override
    public NonDataSelectionPageBuilder<FinishType> thenSelect(String prompt)
    {
        return this.<SelectionPageController<Object>>thenSelect(prompt, null);
    }
}
