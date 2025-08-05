package kintsugi3d.builder.javafx.controllers.paged;

public interface DataSourcePage<T, ControllerType extends PageController<?>>
    extends ReadonlyDataSourcePage<T, ControllerType>
{
    void setData(T data);
}
