package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.internal.ObservableProjectModel;
import kintsugi3d.builder.javafx.internal.ProjectModelBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SelectToneCalibrationImageController extends FXMLPageController
{
    private static final Logger log = LoggerFactory.getLogger(SelectToneCalibrationImageController.class);

    @FXML private AnchorPane anchorPane;

    @FXML private ToggleButton primaryViewImageButton;
    @FXML private ToggleButton previousImageButton;
    @FXML private ToggleButton selectImageFileButton;
    @FXML private Label selectImageFileLabel;

    private FileChooser imageFileChooser;
    private File selectedImageFile = null;

    private final ToggleGroup buttonGroup = new ToggleGroup();

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

        selectImageFileLabel.setVisible(selectImageFileButton.isSelected());
        buttonGroup.selectedToggleProperty().addListener((a, b, c) ->
        {
            selectImageFileLabel.setVisible(selectImageFileButton.isSelected());
        });
    }

    @Override
    public void refresh()
    {
        ObservableProjectModel project = (ObservableProjectModel) MultithreadModels.getInstance().getProjectModel();

        boolean hasPreviousColorCheckerImage = project.getColorCheckerFile() != null && !project.getColorCheckerFile().isEmpty();
        previousImageButton.setDisable(!hasPreviousColorCheckerImage);
    }

    @Override
    public void nextButtonPressed()
    {
        File imageFile = null;
        if (buttonGroup.getSelectedToggle() == primaryViewImageButton)
        {
            ViewSet viewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();
            int primaryViewIndex = viewSet.getPrimaryViewIndex();
            imageFile = viewSet.getFullResImageFile(primaryViewIndex);
        }
        else if (buttonGroup.getSelectedToggle() == selectImageFileButton)
        {
            imageFile = selectedImageFile;
        }

        if (imageFile != null)
        {
            log.debug("Setting new color calibration image: {}", imageFile);
            ObservableProjectModel project = (ObservableProjectModel) MultithreadModels.getInstance().getProjectModel();
            project.setColorCheckerFile(imageFile.getAbsolutePath());
        }
    }

    private void selectImageFileAction(ActionEvent event)
    {
        // Don't show file chooser when deselecting
        if (! selectImageFileButton.isSelected())
            return;

        File temp = imageFileChooser.showOpenDialog(anchorPane.getScene().getWindow());
        if (temp != null)
        {
            selectedImageFile = temp;
            selectImageFileLabel.setText("Selected: " + temp.getName());
        }
    }
}
