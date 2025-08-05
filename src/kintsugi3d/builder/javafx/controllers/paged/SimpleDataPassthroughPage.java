package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public class SimpleDataPassthroughPage<T, ControllerType extends DataReceiverPageController<T, DataPassthroughPage<T, ControllerType>>>
    extends NonEntryPageBase<DataSourcePage<T, ?>, DataReceiverPage<T, ?>, ControllerType>
    implements DataPassthroughPage<T, ControllerType>
{
    private T data;

    public SimpleDataPassthroughPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void initController()
    {
        PageBase.initController(this);
    }

    @Override
    public void setNextPage(DataReceiverPage<T, ?> page)
    {
        super.setNextPage(page);
        page.setPrevPage(this);
    }

    @Override
    public void receiveData(T data)
    {
        this.data = data;
        this.getController().receiveData(data);
    }

    @Override
    public T getData()
    {
        return this.data;
    }


    @Override
    public void setData(T data)
    {
        this.data = data;
    }
}
