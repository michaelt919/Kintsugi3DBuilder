package kintsugi3d.builder.javafx.controllers.paged;

import javafx.scene.layout.Region;

public interface PageController<PageType extends Page<?>>
{
    PageType getPage();

    void setPage(PageType page);

    /**
     * Returns the outer AnchorPane, VBox, GridPane, etc. for the controller's fxml
     *
     * @return
     */
    Region getRootNode();

    void init();

    void refresh();

    void finish();

    boolean isNextButtonValid();

    void setConfirmCallback(Runnable callback);

    boolean nextButtonPressed();

    boolean closeButtonPressed();
}
