package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

abstract class NonSupplierPageBase<T, ControllerType extends NonSupplierPageController<T>>
    extends PageBase<T, T, ControllerType>
{
    protected NonSupplierPageBase(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void initController()
    {
        this.getController().setPage(this);
        this.getController().initPage();
    }
}
