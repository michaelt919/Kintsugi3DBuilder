package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.sidebar.SearchableTreeView;

public abstract class ViewSelectableBase implements ViewSelectable
{
    protected ViewSelectionModel viewSelectionModel;
    protected SearchableTreeView searchableTreeView;
    private String primaryView;
    private double primaryViewRotation;

    @Override
    public boolean needsRefresh(ViewSelectable oldInstance)
    {
        // always refresh by default (only downside is performance)
        return true;
    }

    @Override
    public void setSearchableTreeView(SearchableTreeView searchableTreeView)
    {
        this.searchableTreeView = searchableTreeView;
    }

    @Override
    public ViewSelectionModel getViewSelectionModel()
    {
        return this.viewSelectionModel;
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
