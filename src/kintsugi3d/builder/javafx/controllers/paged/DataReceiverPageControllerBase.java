package kintsugi3d.builder.javafx.controllers.paged;

/**
 * Base class for controllers that receive data from prior pages
 * but do not require their page to that store a different data object for supplying to following pages.
 * May be attached to a SimpleNonDataPage or a SimpleDataReceiverPage with matching T.
 * (a SimpleDataReceiverPage will forward any received data to subsequent pages).
 * Will only receive data if attached to a SimpleDataReceiverPage.
 * @param <T>
 */
public abstract class DataReceiverPageControllerBase<T>
    extends PageControllerBase<T, Page<?, T>>
    implements NonSupplierPageController<T>
{
}
