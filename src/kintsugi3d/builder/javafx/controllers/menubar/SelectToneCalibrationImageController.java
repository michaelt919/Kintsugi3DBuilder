package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class SelectToneCalibrationImageController extends FXMLPageController
{
    private static final Logger log = LoggerFactory.getLogger(SelectToneCalibrationImageController.class);

    @FXML private AnchorPane anchorPane;

    @FXML private ToggleButton primaryViewImageButton;
    @FXML private ToggleButton previousImageButton;
    @FXML private ToggleButton selectImageFileButton;

    private FileChooser imageFileChooser;
    private File selectedImageFile = null;

    private final ToggleGroup buttonGroup = new ToggleGroup();
    private EyedropperController eyedropperController;

    public SelectToneCalibrationImageController()
    {
        imageFileChooser = new FileChooser();
        imageFileChooser.setTitle("Select tone calibration image");
    }


    @Override
    public Region getHostRegion()
    {
        return anchorPane;
    }

    @Override
    public void init()
    {
        buttonGroup.getToggles().add(primaryViewImageButton);
        buttonGroup.getToggles().add(previousImageButton);
        buttonGroup.getToggles().add(selectImageFileButton);

        buttonGroup.selectToggle(primaryViewImageButton);

        selectImageFileButton.setOnAction(this::selectImageFileAction);
    }

    @Override
    public void refresh()
    { }

    public void setEyedropperController(EyedropperController controller)
    {
        this.eyedropperController = controller;
    }

    @Override
    public void nextButtonPressed()
    {
        if (eyedropperController == null)
            return;

        File imageFile = null;
        if (buttonGroup.getSelectedToggle() == primaryViewImageButton)
        {
            ViewSet viewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();
            int primaryViewIndex = viewSet.getPrimaryViewIndex();
            imageFile = MultithreadModels.getInstance().getIOModel().getLoadedViewSet().getFullResImageFile(primaryViewIndex);
        }
        else if (buttonGroup.getSelectedToggle() == previousImageButton)
        {

        }
        else if (buttonGroup.getSelectedToggle() == selectImageFileButton)
        {
            imageFile = selectedImageFile;
        }

        log.debug("Using image for tone calibration eyedropper: {}", imageFile);
        eyedropperController.setImage(imageFile);
    }

    private void selectImageFileAction(ActionEvent event)
    {
        File temp = imageFileChooser.showOpenDialog(anchorPane.getScene().getWindow());
        if (temp != null)
        {
            selectedImageFile = temp;
        }
    }
}
