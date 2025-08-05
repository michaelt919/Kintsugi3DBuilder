package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

abstract class NonEntryPageBase<
        PrevPageType extends Page<?>,
        NextPageType extends Page<?>,
        ControllerType extends PageController<?>>
    extends PageBase<PrevPageType, NextPageType, ControllerType> implements NonEntryPage<PrevPageType, ControllerType>
{
    private PrevPageType prev;

    protected NonEntryPageBase(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public final PrevPageType getPrevPage()
    {
        return prev;
    }

    @Override
    public final boolean hasPrevPage()
    {
        return prev != null;
    }

    @Override
    public void setPrevPage(PrevPageType page)
    {
        this.prev = page;
    }
}
