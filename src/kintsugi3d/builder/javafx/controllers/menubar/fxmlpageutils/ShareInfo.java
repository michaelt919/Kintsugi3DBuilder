package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

public interface ShareInfo {
    enum Info{
        OBJ_FILE,
        METASHAPE_OBJ_CHUNK,
        CAM_FILE,
        PHOTO_DIR,
        PRIMARY_VIEW
    }

    /**
     * Send info to FXML scroller controller upon hitting the "Next" button.
     * Useful for sending information across pages within a single scroller.
     */
    void shareInfo();
}
