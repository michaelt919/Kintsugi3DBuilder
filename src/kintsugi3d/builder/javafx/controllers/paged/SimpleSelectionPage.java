package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public final class SimpleSelectionPage<T, ControllerType extends SelectionPageController<T>>
    extends SelectionPageBase<T, ControllerType>
{
    private T data;

    SimpleSelectionPage(String fxmlFile, FXMLLoader loader)
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
