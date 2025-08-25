package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public final class SimpleNonDataSelectionPage<ControllerType extends SelectionPageController<Object>>
    extends SelectionPageBase<Object, ControllerType>
{
    SimpleNonDataSelectionPage(FXMLLoader loader)
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
