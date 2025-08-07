package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public final class SimpleDataReceiverPage<T> extends NonSupplierPageBase<T>
{
    private T data;

    public SimpleDataReceiverPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void receiveData(T data)
    {
        super.receiveData(data);
        this.data = data;
    }

    @Override
    public T getOutData()
    {
        return data;
    }
}
