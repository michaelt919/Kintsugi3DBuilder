package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import javafx.stage.Window;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;

public abstract class ViewSelectableBase implements ViewSelectable
{
    private Window modalWindow;
    private ViewSelectionModel viewSelectionModel;

    private String primaryView;
    private double primaryViewRotation;

    @Override
    public boolean needsRefresh(ViewSelectable oldInstance)
    {
        // always refresh by default (only downside is performance)
        return true;
    }

    @Override
    public String getAdvanceLabelOverride()
    {
        return "Skip";
    }

    @Override
    public Window getModalWindow()
    {
        return modalWindow;
    }

    @Override
    public void setModalWindow(Window modalWindow)
    {
        this.modalWindow = modalWindow;
    }

    @Override
    public ViewSelectionModel getViewSelectionModel()
    {
        return this.viewSelectionModel;
    }

    protected void setViewSelectionModel(ViewSelectionModel viewSelectionModel)
    {
        this.viewSelectionModel = viewSelectionModel;
    }

    @Override
    public String getViewSelection()
    {
        return primaryView;
    }

    @Override
    public void selectView(String viewName, double viewRotation)
    {
        this.primaryView = viewName;
        this.primaryViewRotation = viewRotation;
    }

    @Override
    public double getViewRotation()
    {
        return primaryViewRotation;
    }
}
