package kintsugi3d.builder.javafx.controllers.paged;

/**
 * Base class for controllers that require a page that can store data for supplying to following pages
 * and do not receive any data from previous pages (any data received will be discarded).
 * May be attached to a SimpleDataSourcePage with matching T or a SimpleDataTransformerPage with OutType matching T.
 * @param <T>
 */
public abstract class DataSourcePageControllerBase<T>
    extends PageControllerBase<Object, DataSupplierPage<?, T>>
    implements DataSupplierPageController<Object, T>
{
    @Override
    public final void receiveData(Object data)
    {
    }
}
