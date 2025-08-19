package kintsugi3d.builder.javafx.controllers.paged;

import kintsugi3d.builder.javafx.core.JavaFXState;

import java.util.function.Supplier;

public class DataPageBuilder<InType, OutType> extends PageBuilder<InType, OutType>
{
    DataPageBuilder(Page<InType, OutType> page, PageFrameController frameController, JavaFXState state, Runnable callback)
    {
        super(page, frameController, state, callback);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder thenNonData(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return super.thenNonData(fxmlPath, controllerConstructorOverride);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder thenNonData(String fxmlPath)
    {
        return super.<ControllerType>thenNonData(fxmlPath);
    }

    public DataPageBuilder<InType, OutType> withDefault(InType data)
    {
        this.page.receiveData(data);
        return this;
    }
}
