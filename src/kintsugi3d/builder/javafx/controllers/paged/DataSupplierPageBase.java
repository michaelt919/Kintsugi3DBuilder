package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

/**
 * Internal base class that is extended for the public-facing classes
 * @param <InType>
 * @param <OutType>
 */
class DataSupplierPageBase<InType, OutType>
    extends PageBase<InType, OutType, DataSupplierPageController<? super InType, OutType>>
    implements DataSupplierPage<InType, OutType>
{
    private OutType data;

    public DataSupplierPageBase(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public final void initController()
    {
        this.getController().setPage(this);
        this.getController().init();
    }

    @Override
    public final OutType getOutData()
    {
        return this.data;
    }

    @Override
    public final void setOutData(OutType data)
    {
        this.data = data;
    }
}
