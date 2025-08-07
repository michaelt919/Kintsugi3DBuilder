package kintsugi3d.builder.javafx.controllers.paged;

public interface DataSupplierPage<InType, OutType>
    extends Page<InType, OutType>
{
    void setOutData(OutType data);
}
