package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

public interface ShareInfo {
    enum Info{
        CAM_FILE,
        MESH_FILE,
        PHOTO_DIR,
        METASHAPE_OBJ_CHUNK,
        PRIMARY_VIEW
    }

    /**
     * Send info to FXML scroller controller upon hitting the "Next" button.
     * Useful for sending information across pages within a single scroller.
     */
    void shareInfo();
}
