package kintsugi3d.builder.javafx.controllers.paged;

public interface DataReceiverPageController<T, PageType extends DataReceiverPage<T, ? extends DataReceiverPageController<T, PageType>>>
    extends PageController<PageType>
{
    void receiveData(T data);
}