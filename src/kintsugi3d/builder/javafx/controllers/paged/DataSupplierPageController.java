package kintsugi3d.builder.javafx.controllers.paged;

public interface DataSupplierPageController<InType, OutType> extends PageController<InType>
{
    @Override
    DataSupplierPage<?, OutType> getPage();

    void setPage(DataSupplierPage<?, OutType> page);
}
