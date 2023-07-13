package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.scene.image.Image;

public class SharedDataModel {
    private static SharedDataModel instance;
    private Image selectedImage;

    private SharedDataModel() {
    }

    public static SharedDataModel getInstance() {
        if (instance == null) {
            instance = new SharedDataModel();
        }
        return instance;
    }

    public Image getSelectedImage() {
        return selectedImage;
    }

    public void setSelectedImage(Image selectedImage) {
        this.selectedImage = selectedImage;
    }
}
