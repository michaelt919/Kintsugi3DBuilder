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
