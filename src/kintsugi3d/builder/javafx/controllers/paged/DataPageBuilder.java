package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public class DataPageBuilder<InType, OutType, FinishType> extends SimplePageBuilder<InType, OutType, FinishType>
{
    DataPageBuilder(Page<? super InType, OutType> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(page, frameController, finisher);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<? super OutType>>
    DataPageBuilder<OutType, OutType, FinishType> then(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return then(fxmlPath, SimpleDataReceiverPage<OutType, ControllerType>::new, controllerConstructorOverride);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<? super OutType>>
    DataPageBuilder<OutType, OutType, FinishType> then(String fxmlPath)
    {
        return this.<ControllerType>then(fxmlPath, null);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return super.thenNonData(fxmlPath, controllerConstructorOverride);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<? super Object>>
    NonDataPageBuilder<FinishType> thenNonData(String fxmlPath)
    {
        return super.<ControllerType>thenNonData(fxmlPath);
    }

    @Override
    public <ControllerType extends SelectionPageController<Object>> NonDataSelectionPageBuilder<FinishType> thenSelectNonData(String prompt, Supplier<ControllerType> controllerConstructorOverride)
    {
        return super.thenSelectNonData(prompt, controllerConstructorOverride);
    }

    @Override
    public NonDataSelectionPageBuilder<FinishType> thenSelectNonData(String prompt)
    {
        return super.thenSelectNonData(prompt);
    }
}
