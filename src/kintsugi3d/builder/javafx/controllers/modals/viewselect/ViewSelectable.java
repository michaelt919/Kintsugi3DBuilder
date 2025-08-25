package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.sidebar.SearchableTreeView;

import java.util.function.Consumer;

public interface ViewSelectable
{
    boolean needsRefresh(ViewSelectable oldInstance);

    void setSearchableTreeView(SearchableTreeView searchableTreeView);

    void loadForViewSelection(Consumer<ViewSelectionModel> onLoadComplete);

    ViewSelectionModel getViewSelectionModel();

    String getViewSelection();
    double getViewRotation();
    void selectView(String viewName, double viewRotation);

    void confirm();
}
