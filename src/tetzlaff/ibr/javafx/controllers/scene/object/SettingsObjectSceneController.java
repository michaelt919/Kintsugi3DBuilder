package tetzlaff.ibr.javafx.controllers.scene.object;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import tetzlaff.ibr.javafx.util.SafeNumberStringConverter;
import tetzlaff.ibr.javafx.util.StaticUtilities;

public class SettingsObjectSceneController implements Initializable
{

    @FXML private VBox root;

    @FXML private TextField xCenterTextField;
    @FXML private TextField yCenterTextField;
    @FXML private TextField zCenterTextField;

    @FXML private Slider xCenterSlider;
    @FXML private Slider yCenterSlider;
    @FXML private Slider zCenterSlider;

    @FXML private TextField rotateYTextField;
    @FXML private Slider rotateYSlider;
    @FXML private TextField rotateXTextField;
    @FXML private Slider rotateXSlider;
    @FXML private TextField rotateZTextField;
    @FXML private Slider rotateZSlider;
    @FXML private Button selectPointButton;

    private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        StaticUtilities.wrap(-180, 180, rotateYTextField);
        StaticUtilities.bound(-90, 90, rotateXTextField);
        StaticUtilities.wrap(-180, 180, rotateZTextField);

        StaticUtilities.cleanInput(xCenterTextField);
        StaticUtilities.cleanInput(yCenterTextField);
        StaticUtilities.cleanInput(zCenterTextField);
    }

    public final ChangeListener<ObjectPoseSetting> changeListener =
        (observable, oldValue, newValue) ->
        {
            if (oldValue != null)
            {
                unbind(oldValue);
            }

            if (newValue != null)
            {
                bind(newValue);
                setDisabled(newValue.isLocked());
            }
            if (newValue == null)
            {
                setDisabled(true);
            }
        };

    public void setDisabled(Boolean disabled)
    {
        root.setDisable(disabled);
    }

    private void bind(ObjectPoseSetting objectPose)
    {

        xCenterTextField.textProperty().bindBidirectional(objectPose.centerXProperty(), n);
        yCenterTextField.textProperty().bindBidirectional(objectPose.centerYProperty(), n);
        zCenterTextField.textProperty().bindBidirectional(objectPose.centerZProperty(), n);
        rotateYTextField.textProperty().bindBidirectional(objectPose.rotateYProperty(), n);
        rotateXTextField.textProperty().bindBidirectional(objectPose.rotateXProperty(), n);
        rotateZTextField.textProperty().bindBidirectional(objectPose.rotateZProperty(), n);

        xCenterSlider.valueProperty().bindBidirectional(objectPose.centerXProperty());
        yCenterSlider.valueProperty().bindBidirectional(objectPose.centerYProperty());
        zCenterSlider.valueProperty().bindBidirectional(objectPose.centerZProperty());
        rotateYSlider.valueProperty().bindBidirectional(objectPose.rotateYProperty());
        rotateXSlider.valueProperty().bindBidirectional(objectPose.rotateXProperty());
        rotateZSlider.valueProperty().bindBidirectional(objectPose.rotateZProperty());
    }

    private void unbind(ObjectPoseSetting objectPose)
    {

        xCenterTextField.textProperty().unbindBidirectional(objectPose.centerXProperty());
        yCenterTextField.textProperty().unbindBidirectional(objectPose.centerYProperty());
        zCenterTextField.textProperty().unbindBidirectional(objectPose.centerZProperty());
        rotateYTextField.textProperty().unbindBidirectional(objectPose.rotateYProperty());
        rotateXTextField.textProperty().unbindBidirectional(objectPose.rotateXProperty());
        rotateZTextField.textProperty().unbindBidirectional(objectPose.rotateZProperty());

        xCenterSlider.valueProperty().unbindBidirectional(objectPose.centerXProperty());
        yCenterSlider.valueProperty().unbindBidirectional(objectPose.centerYProperty());
        zCenterSlider.valueProperty().unbindBidirectional(objectPose.centerZProperty());
        rotateYSlider.valueProperty().unbindBidirectional(objectPose.rotateYProperty());
        rotateXSlider.valueProperty().unbindBidirectional(objectPose.rotateXProperty());
        rotateZSlider.valueProperty().unbindBidirectional(objectPose.rotateZProperty());
    }

    public void setOnActionSelectPoint(EventHandler<ActionEvent> actionEventEventHandler)
    {
        selectPointButton.setOnAction(actionEventEventHandler);
    }
}
