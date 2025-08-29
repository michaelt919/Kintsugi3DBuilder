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

    /**
     * Called when the page is created, after the page hsa been assigned to the controller.
     * In contrast with initialize() (which is called after JavaFX initialization, before any page object has been initialized)
     * when initPage() is called, a valid page will have been assigned to this controller.
     */
    void initPage();

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
    boolean confirm();

    /**
     * Called when the window should close.
     * @return false if closing was cancelled by the controller; true otherwise.
     */
    boolean cancel();
}
