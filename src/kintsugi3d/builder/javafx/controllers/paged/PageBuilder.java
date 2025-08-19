package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class PageBuilder<InType, OutType>
{
    private final Page<InType, OutType> page;
    private final PageFrameController frameController;
    private final JavaFXState state;
    private final Runnable callback;

    PageBuilder(Page<InType, OutType> page, PageFrameController frameController, JavaFXState state, Runnable callback)
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
        return new PageBuilder<>(nextPage, frameController, state, callback);
    }

    public <PageType extends Page<OutType, NextOutType>, NextOutType>
    PageBuilder<OutType, NextOutType> then(String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return then(fxmlPath, pageConstructor, null);
    }

    public <ControllerType extends NonSupplierPageController<Object>>
    PageBuilder<?, Object> thenNonData(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataPage<ControllerType> nextPage = frameController.createPage(
            fxmlPath, SimpleNonDataPage<ControllerType>::new, controllerConstructorOverride);
        this.page.setNextPage(nextPage);
        return new PageBuilder<>(nextPage, frameController, state, callback);
    }

    public <ControllerType extends NonSupplierPageController<Object>>
    PageBuilder<?, Object> thenNonData(String fxmlPath, @SuppressWarnings("unused") Class<ControllerType> controllerClass)
    {
        return thenNonData(fxmlPath, (Supplier<? extends NonSupplierPageController<Object>>) null);
    }

    public PageBuilder<InType, OutType> with(InType data)
    {
        this.page.receiveData(data);
        return this;
    }

    public PageFrameController finish()
    {
        frameController.init(state);
        callback.run();
        return frameController;
    }
}
