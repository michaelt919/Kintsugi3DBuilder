package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public class SimpleDataSourcePage<T, ControllerType extends PageController<DataSourcePage<T, ControllerType>>>
    extends NonEntryPageBase<NonDataSourcePage<?>, DataReceiverPage<T, ?>, ControllerType>
    implements NonDataReceiverPage<ControllerType>, DataSourcePage<T, ControllerType>
{
    private T data;

    public SimpleDataSourcePage(String fxmlFile, FXMLLoader loader)
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
    public T getData()
    {
        return data;
    }

    @Override
    public void setData(T data)
    {
        this.data = data;
    }
}
