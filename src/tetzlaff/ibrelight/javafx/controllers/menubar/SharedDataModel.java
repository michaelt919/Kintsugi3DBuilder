package tetzlaff.ibrelight.javafx.controllers.menubar;

import java.io.File;

public class SharedDataModel {
    private static SharedDataModel instance;
    private File selectedImage;

    private SharedDataModel() {
    }

    public static SharedDataModel getInstance() {
        if (instance == null) {
            instance = new SharedDataModel();
        }
        return instance;
    }

    public File getSelectedImage() {
        return selectedImage;
    }

    public void setSelectedImage(File selectedImage) {
        this.selectedImage = selectedImage;
    }
}
