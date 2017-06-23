package tetzlaff.ibr.alexkautz_workspace.user_interface;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Loader implements Initializable{
    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private BorderPane root;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();

    private File cameraFile = null;
    private File objFile = null;
    private File photoDir = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setHomeDir(new File(System.getProperty("user.home")));
        camFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Agisoft Photoscan XML file", "*.xml"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        objFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Wavefront OBJ file", "*.obj"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        camFileChooser.setTitle("Select camera positions file");
        objFileChooser.setTitle("Select object file");
        photoDirectoryChooser.setTitle("Select undistorted photo directory");
    }

    @FXML private void camFileSelect(){

        File temp = camFileChooser.showOpenDialog(getStage());

        if(temp != null){
            cameraFile = temp;
            setHomeDir(temp);
            loadCheckCameras.setText("Loaded");
            loadCheckCameras.setFill(Paint.valueOf("Green"));
        }

    }

    @FXML private void objFileSelect(){

        File temp = objFileChooser.showOpenDialog(getStage());

        if(temp != null){
            objFile = temp;
            setHomeDir(temp);
            loadCheckObj.setText("Loaded");
            loadCheckObj.setFill(Paint.valueOf("Green"));
        }

    }

    @FXML private void photoDirectorySelect(){

        File temp = photoDirectoryChooser.showDialog(getStage());

        if(temp != null){
            photoDir = temp;
            setHomeDir(temp);
            loadCheckImages.setText("Loaded");
            loadCheckImages.setFill(Paint.valueOf("Green"));
        }
    }

    @FXML private void okButtonPress(){

        if ((cameraFile != null) & (objFile != null) & (photoDir != null)) {

            //ok!

            //TODO pass the files

            close();

        } else {
            //TODO play sound or popup
        }

    }

    @FXML private void cancleButtonPress(){
        close();
    }

    private void close(){
        getStage().close();
    }


    private void setHomeDir(File home){
        File thing;
        if(home.isDirectory()) thing = home;
        else thing = home.getParentFile();
        camFileChooser.setInitialDirectory(thing);
        objFileChooser.setInitialDirectory(thing);
        photoDirectoryChooser.setInitialDirectory(thing);
    }

    private Stage getStage(){
        if(thisStage == null) thisStage = (Stage) root.getScene().getWindow();
        return thisStage;
    }

}
