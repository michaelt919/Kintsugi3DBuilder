package kintsugi3d.builder.javafx.controllers.paged;

import kintsugi3d.builder.javafx.core.JavaFXState;

import java.util.function.Supplier;

public class NonDataPageBuilder extends PageBuilder<Object, Object>
{
    public NonDataPageBuilder(Page<Object, Object> page, PageFrameController frameController, JavaFXState state, Runnable callback)
    {
        super(page, frameController, state, callback);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder then(String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return super.thenNonData(fxmlPath, controllerConstructorOverride);
    }

    @Override
    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder then(String fxmlPath)
    {
        return super.<ControllerType>thenNonData(fxmlPath);
    }
}
