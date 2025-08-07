package kintsugi3d.builder.javafx.controllers.paged;

/**
 * Base class for controllers that require a page that can receive one data object from prior pages
 * and store another data object for supplying to following pages.
 * May be attached to a SimpleDataSourcePage with T matching OutType
 * or a SimpleDataTransformerPage with matching InType and OutType.
 * Will only receive data if attached to a SimpleDataTransformerPage.
 * @param <InType>
 * @param <OutType>
 */
public abstract class DataTransformerPageControllerBase<InType, OutType>
    extends PageControllerBase<InType, DataSupplierPage<?, OutType>>
    implements DataSupplierPageController<InType, OutType>
{
}
