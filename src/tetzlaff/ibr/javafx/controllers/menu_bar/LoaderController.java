package tetzlaff.ibr.javafx.controllers.menu_bar;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import tetzlaff.ibr.javafx.models.JavaFXModelAccess;
import tetzlaff.ibr.javafx.util.StaticUtilities;

public class LoaderController implements Initializable
{

    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private BorderPane root;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();

    private File cameraFile;
    private File objFile;
    private File photoDir;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        setHomeDir(new File(System.getProperty("user.home")));
        camFileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("Agisoft Photoscan XML file", "*.xml"),
            new ExtensionFilter("All Files", "*.*")
        );
        objFileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("Wavefront OBJ file", "*.obj"),
            new ExtensionFilter("All Files", "*.*")
        );

        camFileChooser.setTitle("Select camera positions file");
        objFileChooser.setTitle("Select object file");
        photoDirectoryChooser.setTitle("Select undistorted photo directory");
    }

    @FXML
    private void camFileSelect()
    {

        File temp = camFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            cameraFile = temp;
            setHomeDir(temp);
            loadCheckCameras.setText("Loaded");
            loadCheckCameras.setFill(Paint.valueOf("Green"));
        }
    }

    @FXML
    private void objFileSelect()
    {

        File temp = objFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            objFile = temp;
            setHomeDir(temp);
            loadCheckObj.setText("Loaded");
            loadCheckObj.setFill(Paint.valueOf("Green"));
        }
    }

    @FXML
    private void photoDirectorySelect()
    {

        File temp = photoDirectoryChooser.showDialog(getStage());

        if (temp != null)
        {
            photoDir = temp;
            setHomeDir(temp);
            loadCheckImages.setText("Loaded");
            loadCheckImages.setFill(Paint.valueOf("Green"));
        }
    }

    @FXML
    private void okButtonPress()
    {

        if ((cameraFile != null) & (objFile != null) & (photoDir != null))
        {

            //ok!
            new Thread(() ->
            {
                try
                {
                    JavaFXModelAccess.getInstance().getLoadingModel().loadFromAgisoftFiles(cameraFile.getPath(), cameraFile, objFile, photoDir);
                }
                catch (FileNotFoundException e)
                {
                    System.out.println("files were malformed");
                }
            }).start();

            close();
        }
        else
        {
            //TODO play sound or popup
        }
    }

    @FXML
    private void cancelButtonPress()
    {
        close();
    }

    private void close()
    {
        StaticUtilities.naturalClose(root.getScene().getWindow());
    }

    private void setHomeDir(File home)
    {
        File parentDir;
        parentDir = home.getParentFile();
        camFileChooser.setInitialDirectory(parentDir);
        objFileChooser.setInitialDirectory(parentDir);
        photoDirectoryChooser.setInitialDirectory(parentDir);
    }

    private Stage getStage()
    {
        if (thisStage == null)
        {
            thisStage = (Stage) root.getScene().getWindow();
        }
        return thisStage;
    }

    private final String quickFilename = "quickSaveLoadConfig.txt";

    @FXML
    private void quickSave()
    {
        if ((cameraFile != null) & (objFile != null) & (photoDir != null))
        {
            System.out.println("Quick save");

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(quickFilename)))
            {

                String toWrite =
                    cameraFile.getPath()
                        + "\n" +
                        objFile.getPath()
                        + "\n" +
                        photoDir.getPath();

                bw.write(toWrite);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void quickLoad()
    {
        System.out.println("Quick load");

        try (BufferedReader br = new BufferedReader(new FileReader(quickFilename)))
        {

            Stream<String> lineStream = br.lines();

            String[] lineArray = lineStream.toArray(String[]::new);

            File newCam = new File(lineArray[0]);
            File newObj = new File(lineArray[1]);
            File newPhoto = new File(lineArray[2]);

            if ((newCam != null) & (newObj != null) & (newPhoto != null))
            {
                System.out.println("Loaded");
                cameraFile = newCam;
                objFile = newObj;
                photoDir = newPhoto;

                setHomeDir(cameraFile);
                loadCheckCameras.setText("Loaded");
                loadCheckCameras.setFill(Paint.valueOf("Green"));
                loadCheckObj.setText("Loaded");
                loadCheckObj.setFill(Paint.valueOf("Green"));
                loadCheckImages.setText("Loaded");
                loadCheckImages.setFill(Paint.valueOf("Green"));
            }
            else
            {
                System.out.println("failed");
            }
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Can't find the file (but that's ok)");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
