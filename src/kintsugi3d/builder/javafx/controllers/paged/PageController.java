package kintsugi3d.builder.javafx.controllers.paged;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.scene.layout.Region;

public interface PageController<T>
{
    Page<?, ?> getPage();

    PageFrameController getPageFrameController();
    void setPageFrameController(PageFrameController scroller);

    /**
     * Returns the outer AnchorPane, VBox, GridPane, etc. for the controller's fxml
     *
     * @return
     */
    Region getRootNode();

    BooleanExpression getCanAdvanceObservable();
    boolean canAdvance();

    StringExpression getAdvanceLabelOverrideObservable();
    String getAdvanceLabelOverride();

    BooleanExpression getCanConfirmObservable();
    boolean canConfirm();

    Runnable getConfirmCallback();
    void setConfirmCallback(Runnable callback);

    boolean isConfirmed();

    /**
     * Called when the page is created.
     */
    void init();

    /**
     * Called when the previous page has finished if the previous page has data to share with this page
     * and this page is set up to receive data to forward to the controller.
     * @param data Data received from the previous page.
     */
    void receiveData(T data);

    /**
     * Called when the page is displayed, either through forward or backwards navigation.
     */
    void refresh();

    /**
     * Called when advancing from this page.
     * @return false if navigation was cancelled by the controller; true otherwise.
     */
    boolean advance();

    /**
     * Called after advancing if there are no additional pages and the paged experience is thus complete.
     * @return false if confirmation was cancelled by the controller; true otherwise.
     */
    default boolean confirm()
    {
        return true;
    }

    /**
     * Called when the window should close.
     * @return false if closing was cancelled by the controller; true otherwise.
     */
    boolean close();
}
