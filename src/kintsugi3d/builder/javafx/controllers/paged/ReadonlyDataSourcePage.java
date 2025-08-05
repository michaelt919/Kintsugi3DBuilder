package kintsugi3d.builder.javafx.controllers.paged;

public interface ReadonlyDataSourcePage<T, ControllerType extends PageController<?>>
    extends Page<ControllerType>
{
    T getData();

    @Override
    DataReceiverPage<T, ?> getNextPage();

    /**
     * Should also update the back link from the next page to this.
     * @param page
     */
    void setNextPage(DataReceiverPage<T, ?> page);
}
