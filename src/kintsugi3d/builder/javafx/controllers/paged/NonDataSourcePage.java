package kintsugi3d.builder.javafx.controllers.paged;

public interface NonDataSourcePage<ControllerType extends PageController<?>> extends Page<ControllerType>
{
    @Override
    NonDataReceiverPage<?> getNextPage();

    /**
     * Should also update the back link from the next page to this.
     * @param page
     */
    void setNextPage(NonDataReceiverPage<?> page);
}
