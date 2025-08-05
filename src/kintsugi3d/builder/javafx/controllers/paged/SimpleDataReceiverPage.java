package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public class SimpleDataReceiverPage<T, ControllerType extends DataReceiverPageController<T, DataReceiverPage<T, ControllerType>>>
    extends NonEntryPageBase<DataSourcePage<T, ?>, NonDataReceiverPage<?>, ControllerType>
    implements NonDataSourcePage<ControllerType>, DataReceiverPage<T, ControllerType>
{
    public SimpleDataReceiverPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void initController()
    {
        PageBase.initController(this);
    }

    @Override
    public void receiveData(T data)
    {
        this.getController().receiveData(data);
    }

    @Override
    public void setNextPage(NonDataReceiverPage<?> page)
    {
        super.setNextPage(page);
        page.setPrevPage(this);
    }
}
