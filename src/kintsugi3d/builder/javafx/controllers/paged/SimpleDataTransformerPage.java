package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public final class SimpleDataTransformerPage<InType, OutType, ControllerType extends DataSupplierPageController<? super InType, OutType>>
    extends DataSupplierPageBase<InType, OutType, ControllerType>
{
    public SimpleDataTransformerPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }
}
