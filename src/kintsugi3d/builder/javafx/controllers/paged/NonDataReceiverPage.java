package kintsugi3d.builder.javafx.controllers.paged;

public interface NonDataReceiverPage<ControllerType extends PageController<?>>
    extends NonEntryPage<NonDataSourcePage<?>, ControllerType>
{
    @Override
    NonDataSourcePage<?> getPrevPage();

    void setPrevPage(NonDataSourcePage<?> page);
}
