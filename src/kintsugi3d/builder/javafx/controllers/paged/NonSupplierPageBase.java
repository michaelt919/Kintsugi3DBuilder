package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

abstract class NonSupplierPageBase<T, ControllerType extends NonSupplierPageController<? super T>>
    extends PageBase<T, T, ControllerType>
{
    protected NonSupplierPageBase(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void initController()
    {
        Page<?, T> test;
        test = this;
        this.getController().setPage(test);
        this.getController().initPage();
    }
}
