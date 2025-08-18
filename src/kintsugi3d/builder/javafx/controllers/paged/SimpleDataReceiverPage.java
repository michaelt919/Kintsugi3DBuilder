package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public final class SimpleDataReceiverPage<T, ControllerType extends NonSupplierPageController<T>>
    extends NonSupplierPageBase<T, ControllerType>
{
    private T data;

    public SimpleDataReceiverPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public SimpleDataReceiverPage<T, ControllerType> receiveData(T data)
    {
        super.receiveData(data);
        this.data = data;
        return this;
    }

    @Override
    public T getOutData()
    {
        return data;
    }
}
