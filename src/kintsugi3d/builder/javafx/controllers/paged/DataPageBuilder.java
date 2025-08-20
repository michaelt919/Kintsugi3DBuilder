package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public class DataPageBuilder<InType, OutType, FinishType> extends SimplePageBuilder<InType, OutType, FinishType>
{
    DataPageBuilder(Page<InType, OutType> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(page, frameController, finisher);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return super.thenNonData(fxmlPath, controllerConstructorOverride);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath)
    {
        return super.<ControllerType>thenNonData(fxmlPath);
    }

    public DataPageBuilder<InType, OutType, FinishType> withDefault(InType data)
    {
        this.page.receiveData(data);
        return this;
    }
}
