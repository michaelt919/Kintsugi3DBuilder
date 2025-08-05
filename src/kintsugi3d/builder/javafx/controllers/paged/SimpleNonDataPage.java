package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public class SimpleNonDataPage<T, ControllerType extends PageController<Page<ControllerType>>>
    extends NonEntryPageBase<NonDataSourcePage<?>, NonDataReceiverPage<?>, ControllerType>
    implements NonDataReceiverPage<ControllerType>, NonDataSourcePage<ControllerType>
{
    public SimpleNonDataPage(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void initController()
    {
        PageBase.initController(this);
    }

    @Override
    public void setNextPage(NonDataReceiverPage<?> page)
    {
        super.setNextPage(page);
        page.setPrevPage(this);
    }
}
