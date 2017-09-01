package tetzlaff.ibr.javafx.controllers.scene.environment_map;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import tetzlaff.ibr.javafx.models.JavaFXEnvironmentMapModel;
import tetzlaff.ibr.javafx.models.JavaFXModelAccess;

public class RootEnvironmentSceneController implements Initializable
{
    @FXML
    private VBox settings;
    @FXML
    private SettingsEnvironmentSceneController settingsController;
    @FXML
    private VBox listControls;
    @FXML
    private ListView<EnvironmentSetting> environmentListView;
    @FXML
    private Button theRenameButton;

    private final boolean useStartingMap = false;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        environmentListView.setItems(JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList());

        environmentListView.getSelectionModel().selectedItemProperty().addListener(settingsController.changeListener);

        ObservableList<EnvironmentSetting> environmentList = JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList();

        if (useStartingMap)
        {
            EnvironmentSetting startingMap = new EnvironmentSetting(
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                1.0,
                0.0,
                Color.WHITE,
                Color.BLACK,
                "Free Environment Map",
                false,
                false
            );

            JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList().add(startingMap);
            environmentListView.getSelectionModel().select(0);
        }

        environmentList.addListener((ListChangeListener<? super EnvironmentSetting>) change ->
        {
            change.next();
            if (change.wasAdded() && change.getAddedSize() == environmentList.size())
            {
                environmentListView.getSelectionModel().select(0);
            }
        });
    }

    public void init(JavaFXEnvironmentMapModel environmentMapModel)
    {
        environmentMapModel.setSelected(environmentListView.getSelectionModel().selectedItemProperty());
    }

    @FXML
    private void newEnvButton()
    {
        if (environmentListView.getSelectionModel().getSelectedItem() == null)
        {

            JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList().add(
                new EnvironmentSetting(
                    false,
                    false,
                    false,
                    false,
                    false,
                    null,
                    null,
                    1.0,
                    0.0,
                    Color.WHITE,
                    Color.BLACK,
                    "New Environment",
                    false,
                    false
                )
            );
        }
        else
        {

            List<EnvironmentSetting> environmentList = JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList();
            environmentList.add(environmentListView.getSelectionModel().getSelectedItem().duplicate());
            environmentListView.getSelectionModel().select(environmentList.size() - 1);
        }
    }

    @FXML
    private void saveEnvButton()
    {
        System.out.println("TODO: saved " + environmentListView.getSelectionModel().getSelectedItem() + " to the library.");
    }

    @FXML
    private void renameEnvButton()
    {
        if (useStartingMap && environmentListView.getSelectionModel().getSelectedIndex() == 0)
        {
            return;
        }

        EventHandler<ActionEvent> oldOnAction = theRenameButton.getOnAction();//backup the old on action event for the rename button

        //set up two buttons and a text field for name entry
        listControls.getChildren().iterator().forEachRemaining(n ->
        {
            n.setDisable(true);
        });
        settings.setDisable(true);

        theRenameButton.setDisable(false);

        int renameIndex = listControls.getChildren().indexOf(theRenameButton);

        TextField renameTextField = new TextField();

        listControls.getChildren().add(renameIndex + 1, renameTextField);

        Button cancelRenameButton = new Button("Cancel");

        listControls.getChildren().add(renameIndex + 2, cancelRenameButton);

        renameTextField.setMaxWidth(Double.MAX_VALUE);

        cancelRenameButton.setMaxWidth(Double.MAX_VALUE);

        //set up to event handlers, one to return the controls back to their original state,
        // and the other to actually perform the rename
        EventHandler<ActionEvent> finishRename = new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                listControls.getChildren().removeAll(renameTextField, cancelRenameButton);
                theRenameButton.setOnAction(oldOnAction);

                listControls.getChildren().iterator().forEachRemaining(n ->
                {
                    n.setDisable(false);
                });

                environmentListView.refresh();

                settings.setDisable(false);
            }
        };

        EventHandler<ActionEvent> doRename = new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                String newName = renameTextField.getText();
                if (!"".equals(newName))
                {
                    environmentListView.getSelectionModel().getSelectedItem().setName(newName);
                }

                finishRename.handle(event);
            }
        };

        //set the on actions
        theRenameButton.setOnAction(doRename);
        cancelRenameButton.setOnAction(finishRename);
        renameTextField.setOnAction(doRename);

        renameTextField.setText(environmentListView.getSelectionModel().getSelectedItem().getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();
    }

    @FXML
    private void moveUPButton()
    {
        int i = environmentListView.getSelectionModel().getSelectedIndex();
        if (useStartingMap && i == 1)
        {
            return;
        }
        if (i > 0)
        {
            Collections.swap(JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList(), i, i - 1);
            environmentListView.getSelectionModel().select(i - 1);
        }
    }

    @FXML
    void moveDOWNButton()
    {
        int i = environmentListView.getSelectionModel().getSelectedIndex();
        if (useStartingMap && i == 0)
        {
            return;
        }

        List<EnvironmentSetting> environmentList = JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList();
        if (i < environmentList.size() - 1)
        {
            Collections.swap(environmentList, i, i + 1);
            environmentListView.getSelectionModel().select(i + 1);
        }
    }

    @FXML
    void lockEnvButton()
    {
        Boolean newValue = !environmentListView.getSelectionModel().getSelectedItem().isLocked();
        environmentListView.getSelectionModel().getSelectedItem().setLocked(newValue);
        settingsController.setDisabled(newValue);
        environmentListView.refresh();
    }

    @FXML
    void deleteEnvButton()
    {
        int i = environmentListView.getSelectionModel().getSelectedIndex();
        if (useStartingMap && i == 0)
        {
            return;
        }
        JavaFXModelAccess.getInstance().getSceneModel().getEnvironmentList().remove(i);
    }
}
