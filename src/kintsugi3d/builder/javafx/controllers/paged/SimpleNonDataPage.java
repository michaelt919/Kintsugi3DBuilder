package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public final class SimpleNonDataPage<ControllerType extends NonSupplierPageController<Object>>
    extends NonSupplierPageBase<Object, ControllerType>
{
    public SimpleNonDataPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    /**
     * Does nothing as the page does not receive data.
     * @param data
     */
    @Override
    public SimpleNonDataPage<ControllerType> receiveData(Object data)
    {
        // Suppress as we don't expect the incoming data to be useful.
        return this;
    }

    /**
     * Always returns null as the page does not supply data.
     * @return null
     */
    @Override
    public Object getOutData()
    {
        return null;
    }
}
