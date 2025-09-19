package kintsugi3d.builder.javafx.controllers.paged;

/**
 * Base class for controllers that do not receive or supply data from previous or subsequent pages.
 * May be attached to a SimpleNonDataPage or a SimpleDataReceiverPage
 * (any data received will be ignored; a SimpleDataReceiverPage will forward any received data to subsequent pages).
 */
public abstract class NonDataPageControllerBase extends PageControllerBase<Object, Page<?, ?>>
    implements NonSupplierPageController<Object>
{
    @Override
    public final void receiveData(Object data)
    {
    }
}
