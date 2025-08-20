package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public class NonDataPageBuilder<FinishType> extends SimplePageBuilder<Object, Object, FinishType>
{
    NonDataPageBuilder(Page<Object, Object> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(page, frameController, finisher);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<FinishType> then(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return super.thenNonData(fxmlPath, controllerConstructorOverride);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<FinishType> then(String fxmlPath)
    {
        return super.<ControllerType>thenNonData(fxmlPath);
    }

    @Override
    public <ControllerType extends SelectionPageControllerBase<? super Object>>
    SelectionPageBuilder<Object, FinishType> thenSelect(Supplier<ControllerType> controllerConstructorOverride)
    {
        return thenSelect(SimpleNonDataSelectionPage<ControllerType>::new, controllerConstructorOverride);
    }

    @Override
    public SelectionPageBuilder<Object, FinishType> thenSelect()
    {
        return this.<SimpleSelectionPageController>thenSelect(null);
    }
}
