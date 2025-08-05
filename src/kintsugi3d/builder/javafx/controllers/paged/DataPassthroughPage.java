package kintsugi3d.builder.javafx.controllers.paged;

public interface DataPassthroughPage<T, ControllerType extends DataReceiverPageController<T, ?>>
    extends DataSourcePage<T, ControllerType>, DataReceiverPage<T, ControllerType>
{
}
