package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class PageBuilder<InType, OutType>
{
    protected final Page<InType, OutType> page;
    protected final PageFrameController frameController;
    protected final JavaFXState state;
    protected final Runnable callback;

    public PageBuilder(Page<InType, OutType> page, PageFrameController frameController, JavaFXState state, Runnable callback)
    {
        this.page = page;
        this.frameController = frameController;
        this.state = state;
        this.callback = callback;
    }

    public <PageType extends Page<OutType, NextOutType>, NextOutType, ControllerType extends PageController<?>>
    PageBuilder<OutType, NextOutType> then(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new DataPageBuilder<>(nextPage, frameController, state, callback);
    }

    public <PageType extends Page<OutType, NextOutType>, NextOutType>
    PageBuilder<OutType, NextOutType> then(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return then(fxmlPath, pageConstructor, null);
    }

    public <ControllerType extends NonSupplierPageController<OutType>>
    PageBuilder<OutType, OutType> then(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return then(fxmlPath, SimpleDataReceiverPage<OutType, ControllerType>::new, controllerConstructorOverride);
    }

    public <ControllerType extends NonSupplierPageController<OutType>>
    PageBuilder<OutType, OutType> then(String fxmlPath)
    {
        return this.<ControllerType>then(fxmlPath, null);
    }

    protected <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder thenNonData(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataPage<ControllerType> nextPage =
            frameController.createPage(fxmlPath, SimpleNonDataPage<ControllerType>::new, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new NonDataPageBuilder(nextPage, frameController, state, callback);
    }

    protected <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder thenNonData(String fxmlPath)
    {
        return this.<ControllerType>thenNonData(fxmlPath, null);
    }

    public PageFrameController finish()
    {
        frameController.init(state);
        callback.run();
        return frameController;
    }
}
