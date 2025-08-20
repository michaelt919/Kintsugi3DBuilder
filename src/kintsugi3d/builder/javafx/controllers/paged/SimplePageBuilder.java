package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class SimplePageBuilder<InType, OutType, FinishType> extends PageBuilder<FinishType>
{
    protected static final String SELECTION_PAGE_FXML = "/fxml/SelectionPage.fxml";

    protected final Page<InType, OutType> page;

    SimplePageBuilder(Page<InType, OutType> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(frameController, finisher);
        this.page = page;
    }

    public <PageType extends Page<OutType, NextOutType>, NextOutType>
    FinishType join(PageType joinPage)
    {
        this.page.setNextPage(joinPage);
        return finisher.get();
    }

    public <PageType extends Page<OutType, NextOutType>, NextOutType, ControllerType extends PageController<?>>
    SimplePageBuilder<OutType,NextOutType, FinishType> then(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new DataPageBuilder<>(nextPage, frameController, finisher);
    }

    public <PageType extends Page<OutType, NextOutType>, NextOutType>
    SimplePageBuilder<OutType,NextOutType, FinishType> then(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return then(fxmlPath, pageConstructor, null);
    }

    public <ControllerType extends NonSupplierPageController<OutType>>
    SimplePageBuilder<OutType, OutType, FinishType> then(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return then(fxmlPath, SimpleDataReceiverPage<OutType, ControllerType>::new, controllerConstructorOverride);
    }

    public <ControllerType extends NonSupplierPageController<OutType>>
    SimplePageBuilder<OutType, OutType, FinishType> then(String fxmlPath)
    {
        return this.<ControllerType>then(fxmlPath, null);
    }

    protected <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataPage<ControllerType> nextPage =
            frameController.createPage(fxmlPath, SimpleNonDataPage<ControllerType>::new, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new NonDataPageBuilder<>(nextPage, frameController, finisher);
    }

    protected <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath)
    {
        return this.<ControllerType>thenNonData(fxmlPath, null);
    }

    public <PageType extends SelectionPage<OutType>, ControllerType extends PageController<? super OutType>>
    SelectionPageBuilder<OutType, FinishType> thenSelect(BiFunction<String, FXMLLoader, PageType> pageConstructor, Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(SELECTION_PAGE_FXML, pageConstructor, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new SelectionPageBuilder<>(nextPage, frameController, finisher);
    }

    public <PageType extends SelectionPage<OutType>, ControllerType extends PageController<? super OutType>>
    SelectionPageBuilder<OutType, FinishType> thenSelect(BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return this.<PageType, ControllerType>thenSelect(pageConstructor, null);
    }

    public <ControllerType extends SelectionPageControllerBase<? super OutType>>
    SelectionPageBuilder<OutType, FinishType> thenSelect(Supplier<ControllerType> controllerConstructorOverride)
    {
        return thenSelect(SimpleSelectionPage<OutType, ControllerType>::new, controllerConstructorOverride);
    }

    public SelectionPageBuilder<OutType, FinishType> thenSelect()
    {
        return this.<SimpleSelectionPageController>thenSelect(null);
    }
}
