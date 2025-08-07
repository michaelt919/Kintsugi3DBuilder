package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

abstract class NonSupplierPageBase<T> extends PageBase<T, T, NonSupplierPageController<T>>
{
    protected NonSupplierPageBase(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void initController()
    {
        this.getController().setPage(this);
        this.getController().init();
    }
}
