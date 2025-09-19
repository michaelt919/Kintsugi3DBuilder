package kintsugi3d.builder.javafx.controllers.paged;

public interface NonSupplierPageController<T> extends PageController<T>
{
    void setPage(Page<?, ? extends T> page);
}
