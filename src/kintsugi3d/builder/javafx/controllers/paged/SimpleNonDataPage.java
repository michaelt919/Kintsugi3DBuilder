package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public class SimpleNonDataPage<ControllerType extends NonSupplierPageController<? super Object>>
    extends NonSupplierPageBase<Object, ControllerType>
{
    public SimpleNonDataPage(FXMLLoader loader)
    {
        super(loader);
    }

    /**
     * Does nothing as the page does not receive data.
     * @param data
     */
    @Override
    public final void receiveData(Object data)
    {
        // Suppress as we don't expect the incoming data to be useful.
    }

    /**
     * Always returns null as the page does not supply data.
     * @return null
     */
    @Override
    public final Object getOutData()
    {
        return null;
    }
}
