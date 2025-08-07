package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public final class SimpleDataTransformerPage<InType, OutType> extends DataSupplierPageBase<InType, OutType>
{
    public SimpleDataTransformerPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }
}
