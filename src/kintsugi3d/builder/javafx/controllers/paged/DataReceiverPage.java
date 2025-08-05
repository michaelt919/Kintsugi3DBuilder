package kintsugi3d.builder.javafx.controllers.paged;

public interface DataReceiverPage<T, ControllerType extends DataReceiverPageController<T, ?>>
    extends NonEntryPage<DataSourcePage<T, ?>, ControllerType>
{
    ControllerType getController();

    void receiveData(T data);

    @Override
    DataSourcePage<T, ?> getPrevPage();
}
