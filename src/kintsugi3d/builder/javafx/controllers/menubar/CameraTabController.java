package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import kintsugi3d.builder.javafx.internal.CameraViewListModelImpl;

import java.io.IOException;
import java.util.List;

public class CameraTabController {

    @FXML private TextField camera_searchbar;
    @FXML private VBox camera_vbox;

    private SearchableListView searchableListView;
    private List<String> cameraViewList;

    public void init(CameraViewListModelImpl cameraViewListModel)
    {
        try {
            cameraViewList = cameraViewListModel.getCameraViewList();
            for (String camera : cameraViewList) {
                camera_vbox.getChildren().add(createDataCard(camera));
            }
            System.out.println("INITIALIZED!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    private HBox createDataCard(String camera) {
        System.out.println("Trying to create datacard");
        HBox newCard = null;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/menubar/leftpanel/TexturePreviewHeader.fxml"));
        try {
            newCard = fxmlLoader.load();
            Text title = (Text) newCard.lookup("#texture_title");
            title.setText(camera);
        } catch (IOException e) {
            // throw new RuntimeException(e);
        }
        return newCard;
    }
}
