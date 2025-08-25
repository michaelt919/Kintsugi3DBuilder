package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

/**
 * Discards any data received and supports storing data to share with the 
 * @param <T>
 */
public final class SimpleDataSourcePage<T, ControllerType extends DataSupplierPageController<Object, T>>
    extends DataSupplierPageBase<Object, T, ControllerType>
{
    public SimpleDataSourcePage(FXMLLoader loader)
    {
        super(loader);
    }

    /**
     * Does nothing as the page does not receive data.
     * @param data
     */
    @Override
    public void receiveData(Object data)
    {
        // Suppress as we don't expect the incoming data to be useful.
    }
}
