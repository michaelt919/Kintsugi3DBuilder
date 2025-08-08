package kintsugi3d.builder.javafx.experience;

import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.controllers.scene.object.ObjectPoseSetting;
import kintsugi3d.builder.javafx.controllers.scene.object.SettingsObjectSceneController;

import java.io.IOException;

public class ObjectOrientation extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Object Orientation";
    }

    @Override
    public void open() throws IOException
    {
        SettingsObjectSceneController objectOrientationController = openModal("fxml/scene/object/SettingsObjectScene.fxml");

        if (objectOrientationController != null)
        {
            ObjectPoseSetting boundObjectPose = getState().getObjectModel().getSelectedObjectPoseProperty().getValue();

            objectOrientationController.bind(boundObjectPose);
            getModal().getStage().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                e -> objectOrientationController.unbind(boundObjectPose));
        }
    }
}
