package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import javafx.stage.Window;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;

import java.util.function.Consumer;

public interface ViewSelectable
{
    boolean needsRefresh(ViewSelectable oldInstance);
    String getAdvanceLabelOverride();

    Window getModalWindow();
    void setModalWindow(Window modalWindow);

    void loadForViewSelection(Consumer<ViewSelectionModel> onLoadComplete);

    ViewSelectionModel getViewSelectionModel();

    String getViewSelection();
    double getViewRotation();
    void selectView(String viewName, double viewRotation);

    void confirm();
}
