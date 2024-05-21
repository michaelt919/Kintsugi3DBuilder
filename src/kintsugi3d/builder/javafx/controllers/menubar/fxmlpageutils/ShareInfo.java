package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

public interface ShareInfo {
    enum Info{
        OBJ_FILE,
        METASHAPE_OBJ_CHUNK,
        CAM_FILE,
        PHOTO_DIR,
        PRIMARY_VIEW
    }
    void shareInfo(); //send info to FXML scroller controller upon hitting the next button
}
