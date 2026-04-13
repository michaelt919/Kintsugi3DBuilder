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

public abstract class SimplePageBuilder<InType, OutType, FinishType> extends PageBuilder<FinishType>
{
    private final Page<? super InType, OutType> page;

    SimplePageBuilder(Page<? super InType, OutType> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(frameController, finisher);
        this.page = page;
    }

    @Override
    public Page<? super InType, OutType> getPage()
    {
        return this.page;
    }

    public <PageType extends Page<? super OutType, NextOutType>, NextOutType>
    FinishType join(PageType joinPage)
    {
        this.page.setNextPage(joinPage);
        return finisher.get();
    }

    public <PageType extends Page<? super OutType, NextOutType>, NextOutType, ControllerType extends PageController<?>>
    SimplePageBuilder<OutType, NextOutType, FinishType> then(String fxmlPath, Function<FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new DataPageBuilder<>(nextPage, frameController, finisher);
    }

    public <PageType extends Page<? super OutType, NextOutType>, NextOutType>
    SimplePageBuilder<OutType, NextOutType, FinishType> then(String fxmlPath, Function<FXMLLoader, PageType> pageConstructor)
    {
        return then(fxmlPath, pageConstructor, null);
    }

    public abstract <ControllerType extends NonSupplierPageController<? super OutType>>
    SimplePageBuilder<OutType, OutType, FinishType> then(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride);

    public abstract <ControllerType extends NonSupplierPageController<? super OutType>>
    SimplePageBuilder<OutType, OutType, FinishType> then(String fxmlPath);

    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataPage<ControllerType> nextPage =
            frameController.createPage(fxmlPath, SimpleNonDataPage<ControllerType>::new, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new NonDataPageBuilder<>(nextPage, frameController, finisher);
    }

    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath)
    {
        return this.<ControllerType>thenNonData(fxmlPath, null);
    }

    public <PageType extends SelectionPage<OutType>, ControllerType extends PageController<OutType>>
    SelectionPageBuilder<? super OutType, FinishType> thenSelect(String prompt, Function<FXMLLoader, PageType> pageConstructor, Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(SELECTION_PAGE_FXML, pageConstructor, controllerConstructorOverride);
        nextPage.setPrompt(prompt);
        this.page.setNextPage(nextPage);
        return new SelectionPageBuilder<>(nextPage, frameController, finisher);
    }

    public <PageType extends SelectionPage<OutType>, ControllerType extends PageController<OutType>>
    SelectionPageBuilder<? super OutType, FinishType> thenSelect(String prompt, Function<FXMLLoader, PageType> pageConstructor)
    {
        return this.<PageType, ControllerType>thenSelect(prompt, pageConstructor, null);
    }

    public <ControllerType extends SelectionPageController<OutType>>
    SelectionPageBuilder<? super OutType, FinishType> thenSelect(String prompt, Supplier<ControllerType> controllerConstructorOverride)
    {
        return thenSelect(prompt, SimpleSelectionPage<OutType, ControllerType>::new, controllerConstructorOverride);
    }

    public <ControllerType extends SelectionPageController<OutType>>
    SelectionPageBuilder<? super OutType, FinishType> thenSelect(String prompt)
    {
        return thenSelect(prompt, SimpleSelectionPage<OutType, ControllerType>::new, null);
    }

    protected <ControllerType extends SelectionPageController<Object>>
    NonDataSelectionPageBuilder<FinishType> thenSelectNonData(String prompt, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataSelectionPage<ControllerType> nextPage = frameController.createPage(
            SELECTION_PAGE_FXML, SimpleNonDataSelectionPage<ControllerType>::new, controllerConstructorOverride);
        nextPage.setPrompt(prompt);
        this.page.setNextPage(nextPage);
        return new NonDataSelectionPageBuilder<>(nextPage, frameController, finisher);
    }

    protected NonDataSelectionPageBuilder<FinishType> thenSelectNonData(String prompt)
    {
        return this.thenSelectNonData(prompt, null);
    }

    public <PageType extends Page<? super Object, NextOutType>, NextOutType, ControllerType extends PageController<?>>
    SimplePageBuilder<Object, NextOutType, SimplePageBuilder<InType, OutType, FinishType>>
    buildFallback(String label, String fxmlPath, Function<FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType fallbackPage = frameController.createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.page.addFallbackPage(label, fallbackPage);
        return new DataPageBuilder<>(fallbackPage, frameController, () -> this);
    }

    public <PageType extends Page<? super Object, NextOutType>, NextOutType>
    SimplePageBuilder<Object, NextOutType, SimplePageBuilder<InType, OutType, FinishType>>
    buildFallback(String label, String fxmlPath, Function<FXMLLoader, PageType> pageConstructor)
    {
        return buildFallback(label, fxmlPath, pageConstructor, null);
    }

    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<SimplePageBuilder<InType, OutType, FinishType>>
    buildFallback(String label, String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataPage<ControllerType> fallbackPage =
            frameController.createPage(fxmlPath, SimpleNonDataPage<ControllerType>::new, controllerConstructorOverride);
        this.page.addFallbackPage(label, fallbackPage);
        return new NonDataPageBuilder<>(fallbackPage, frameController, () -> this);
    }

    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<SimplePageBuilder<InType, OutType, FinishType>> buildFallback(String label, String fxmlPath)
    {
        return this.<ControllerType>buildFallback(label, fxmlPath, null);
    }

    public <PageType extends Page<? super Object, NextOutType>, NextOutType>
    SimplePageBuilder<InType, OutType, FinishType> joinFallback(String label, PageType joinPage)
    {
        this.page.addFallbackPage(label, joinPage);
        return this;
    }
}
