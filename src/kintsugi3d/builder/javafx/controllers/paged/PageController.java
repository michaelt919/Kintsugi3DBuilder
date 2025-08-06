package kintsugi3d.builder.javafx.controllers.paged;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.scene.layout.Region;

public interface PageController<PageType extends Page<?>>
{
    PageType getPage();

    void setPage(PageType page);

    PageFrameController getPageFrameController();

    void setPageFrameController(PageFrameController scroller);

    /**
     * Returns the outer AnchorPane, VBox, GridPane, etc. for the controller's fxml
     *
     * @return
     */
    Region getRootNode();

    void init();

    void refresh();

    boolean advance();

    boolean close();

    BooleanExpression getCanAdvanceObservable();

    default boolean canAdvance()
    {
        return getCanAdvanceObservable().get();
    }

    StringExpression getAdvanceLabelOverrideObservable();

    default String getAdvanceLabelOverride()
    {
        return getAdvanceLabelOverrideObservable().get();
    }

    BooleanExpression getCanConfirmObservable();

    default boolean canConfirm()
    {
        return getCanConfirmObservable().get();
    }

    boolean isConfirmed();

    default boolean confirm()
    {
        return true;
    }

    Runnable getConfirmCallback();

    void setConfirmCallback(Runnable callback);
}
