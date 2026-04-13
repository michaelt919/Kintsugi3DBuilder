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

import java.util.function.Function;
import java.util.function.Supplier;

public class SelectionPageBuilder<T, FinishType> extends PageBuilder<FinishType>
{
    final SelectionPage<T> page;

    SelectionPageBuilder(SelectionPage<T> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(frameController, finisher);
        this.page = page;
    }

    @Override
    public SelectionPage<T> getPage()
    {
        return this.page;
    }

    public <PageType extends Page<? super T, NextOutType>, NextOutType>
    SelectionPageBuilder<? super T, FinishType> choiceJoin(String choiceLabel, PageType joinPage)
    {
        this.page.addChoice(choiceLabel, joinPage);
        return this;
    }

    public <PageType extends Page<? super T, NextOutType>, NextOutType, ControllerType extends PageController<?>>
    SimplePageBuilder<T, NextOutType, SelectionPageBuilder<T,FinishType>> choice(
        String choiceLabel, String fxmlPath, Function<FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.page.addChoice(choiceLabel, nextPage);
        return new DataPageBuilder<>(nextPage, frameController, () -> this);
    }

    public <PageType extends Page<? super T, NextOutType>, NextOutType>
    SimplePageBuilder<T, NextOutType, SelectionPageBuilder<T,FinishType>> choice(
        String choiceLabel, String fxmlPath, Function<FXMLLoader, PageType> pageConstructor)
    {
        return choice(choiceLabel, fxmlPath, pageConstructor, null);
    }

    public <ControllerType extends NonSupplierPageController<T>>
    SimplePageBuilder<T, T, SelectionPageBuilder<T,FinishType>> choice(
        String choiceLabel, String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return choice(choiceLabel, fxmlPath, SimpleDataReceiverPage<T, ControllerType>::new, controllerConstructorOverride);
    }

    public <ControllerType extends NonSupplierPageController<T>>
    SimplePageBuilder<T, T, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath)
    {
        return this.<ControllerType>choice(choiceLabel, fxmlPath, null);
    }

    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<SelectionPageBuilder<T,FinishType>> choiceNonData(
        String choiceLabel, String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataPage<ControllerType> nextPage =
            frameController.createPage(fxmlPath, SimpleNonDataPage<ControllerType>::new, controllerConstructorOverride);
        this.page.addChoice(choiceLabel, nextPage);
        return new NonDataPageBuilder<>(nextPage, frameController, () -> this);
    }

    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<SelectionPageBuilder<T,FinishType>> choiceNonData(String choiceLabel, String fxmlPath)
    {
        return this.<ControllerType>choiceNonData(choiceLabel, fxmlPath, null);
    }

    public <PageType extends SelectionPage<T>, ControllerType extends PageController<T>>
    SelectionPageBuilder<T, FinishType> choiceSubSelect(String choiceLabel, String subprompt,
        Function<FXMLLoader, PageType> pageConstructor, Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(SELECTION_PAGE_FXML, pageConstructor, controllerConstructorOverride);
        nextPage.setPrompt(subprompt);
        this.page.addChoice(choiceLabel, nextPage);
        return new SelectionPageBuilder<>(nextPage, frameController, finisher);
    }

    public <PageType extends SelectionPage<T>, ControllerType extends PageController<T>>
    SelectionPageBuilder<T, FinishType> choiceSubSelect(
        String choiceLabel, String subprompt, Function<FXMLLoader, PageType> pageConstructor)
    {
        return this.<PageType, ControllerType>choiceSubSelect(choiceLabel, subprompt, pageConstructor, null);
    }

    public <ControllerType extends SelectionPageController<T>>
    SelectionPageBuilder<T, FinishType> choiceSubSelect(
        String choiceLabel, String subprompt, Supplier<ControllerType> controllerConstructorOverride)
    {
        return choiceSubSelect(choiceLabel, subprompt, SimpleSelectionPage<T, ControllerType>::new, controllerConstructorOverride);
    }

    public SelectionPageBuilder<T, FinishType> choiceSubSelect(String choiceLabel, String subprompt)
    {
        return this.<SelectionPageController<T>>choiceSubSelect(choiceLabel, subprompt, null);
    }
}
