package tetzlaff.ibr.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.scene.control.skin.TableHeaderRow;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tetzlaff.ibr.javafx.models.JavaFXLightingModel;

public class RootLightSceneController implements Initializable
{
    @FXML private VBox settings;
    @FXML private SettingsLightSceneController settingsController;
    @FXML private TableView<LightGroupSetting> tableView;
    @FXML private VBox groupControls;
    @FXML private VBox lightControls;
    @FXML private Button theRenameButton;

    private final ObservableList<LightGroupSetting> lightGroups = new ObservableListWrapper<>(new ArrayList<>());
    private final Property<LightInstanceSetting> selectedLight = new SimpleObjectProperty<>();
    private int lastSelectedIndex = -1;

    @SuppressWarnings("rawtypes")
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        System.out.println("GLSC started!");

        //TABLE SET UP
        //columns
        TableColumn<LightGroupSetting, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(param ->
        {
            String s = "";
            if (param.getValue().isLocked())
            {
                s = "(L)";
            }
            return new SimpleStringProperty(s + param.getValue().getName());
        });
        tableView.getColumns().add(nameCol);

        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
        {
            final Integer tempFinalInt = i;

            TableColumn<LightGroupSetting, LightInstanceSetting> newCol = new TableColumn<>("L" + (tempFinalInt + 1));

            newCol.setCellValueFactory(param -> param.getValue().lightListProperty().valueAt(tempFinalInt));

            newCol.setPrefWidth(35);

            tableView.getColumns().add(newCol);
        }

        tableView.getSelectionModel().setCellSelectionEnabled(true);

        //light selection listener
        //noinspection rawtypes
        tableView.getSelectionModel().getSelectedCells().addListener((ListChangeListener<TablePosition>) c ->
        {
            while (c.next())
            {
                if (c.wasAdded())
                {
                    //new cell selected
                    assert c.getAddedSize() == 1;
                    TablePosition<?, ?> tb = c.getAddedSubList().get(0);
                    ObservableValue<?> selected = tb.getTableColumn().getCellObservableValue(tb.getRow());
                    if (selected != null && selected.getValue() instanceof LightInstanceSetting)
                    {
                        selectedLight.setValue((LightInstanceSetting) selected.getValue());
                        lastSelectedIndex = tb.getColumn() - 1;
                    }
                    else
                    {
                        selectedLight.setValue(null);
                        lastSelectedIndex = -1;
                    }
                }
            }
        });

        //preventing reordering or rearranging
        tableView.skinProperty().addListener((obs, oldS, newS) ->
        {
            final TableHeaderRow tableHeaderRow = (TableHeaderRow) tableView.lookup("TableHeaderRow");
            tableHeaderRow.reorderingProperty().addListener((p, o, n) ->
                tableHeaderRow.setReordering(false));
        });

        tableView.setSortPolicy(param -> false);

        tableView.setColumnResizePolicy(param -> false);

        tableView.setItems(lightGroups);

        //TABLE SET UP DONE

        //lightGroups.add(new LightGroupSetting("Free Lights"));

        selectedLight.addListener(settingsController.changeListener);
    }

    public void init2(JavaFXLightingModel lightingModel)
    {
        System.out.println("Lights in!");
        ObservableValue<LightGroupSetting> observableValue = tableView.getSelectionModel().selectedItemProperty();
        System.out.println("Setting " + observableValue);
        lightingModel.setLightGroupSettingObservableValue(
            observableValue
        );
    }

    @FXML
    private void newGroup()
    {
        //for now we will create a blank group
        //in the future we may want to duplicate the previous group instead
        LightGroupSetting newGroup = new LightGroupSetting("New Group");
        lightGroups.add(newGroup);
        tableView.getSelectionModel().select(lightGroups.size() - 1, tableView.getColumns().get(0));
    }

    @FXML
    private void saveGroup()
    {
        System.out.println("TODO saveGroup");//TODO
    }

    @FXML
    private void renameGroup()
    {

        EventHandler<ActionEvent> oldOnAction = theRenameButton.getOnAction();//backup the old on action event for the rename button

        //disable all
        groupControls.getChildren().iterator().forEachRemaining(node -> node.setDisable(true));
        lightControls.getChildren().iterator().forEachRemaining(node -> node.setDisable(true));
        settings.setDisable(true);

        theRenameButton.setDisable(false);

        int renameIndex = groupControls.getChildren().indexOf(theRenameButton);

        TextField renameTextField = new TextField();

        groupControls.getChildren().add(renameIndex + 1, renameTextField);

        Button cancelRenameButton = new Button("Cancel");

        groupControls.getChildren().add(renameIndex + 2, cancelRenameButton);

        renameTextField.setMaxWidth(Double.MAX_VALUE);

        cancelRenameButton.setMaxWidth(Double.MAX_VALUE);

        //set up to event handlers, one to return the controls back to their original state,
        //and the other to actually perform the rename
        EventHandler<ActionEvent> finishRename = event ->
        {
            groupControls.getChildren().removeAll(renameTextField, cancelRenameButton);
            theRenameButton.setOnAction(oldOnAction);

            groupControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(false));

            lightControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(false));

//                tableView.refresh();

            settings.setDisable(false);

            tableView.refresh();
        };

        EventHandler<ActionEvent> doRename = event ->
        {
            String newName = renameTextField.getText();
            if (!newName.equals(""))
            {
                tableView.getSelectionModel().getSelectedItem().setName(newName);
            }

            finishRename.handle(event);
        };

        //set the on actions
        theRenameButton.setOnAction(doRename);
        cancelRenameButton.setOnAction(finishRename);
        renameTextField.setOnAction(doRename);

        renameTextField.setText(tableView.getSelectionModel().getSelectedItem().getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();
    }

    @FXML
    private void moveUPGroup()
    {
        if (getRow() > 0)
        {
            Collections.swap(lightGroups, getRow(), getRow() - 1);
        }
    }

    @FXML
    private void moveDOWNGroup()
    {
        if (getRow() < lightGroups.size() - 1)
        {
            Collections.swap(lightGroups, getRow(), getRow() + 1);
        }
    }

    @FXML
    private void lockGroup()
    {
        if (getSelected() != null)
        {
            boolean n = !getSelected().isLocked();
            getSelected().setLocked(n);
            if (selectedLight.getValue() != null)
            {
                settingsController.setDisabled(n | selectedLight.getValue().isLocked());
            }
        }

        tableView.refresh();
    }

    @FXML
    private void keyframeGroup()
    {
        System.out.println("TODO keyframeGroup");//TODO
    }

    @FXML
    private void deleteGroup()
    {
        if (lightGroups.size() > getRow() & getRow() >= 0)
        {
            lightGroups.remove(getRow());
        }
    }

    @FXML
    private void newLight()
    {
        if (getSelected() != null)
        {
            getSelected().addLight(lastSelectedIndex);
        }
        tableView.refresh();
        tableView.getSelectionModel().selectRightCell();
    }

    @FXML
    private void saveLight()
    {
        System.out.println("TODO saveLight");//TODO
    }

    @FXML
    private void lockLight()
    {
        if (selectedLight.getValue() != null)
        {
            boolean newValue = !selectedLight.getValue().isLocked();
            selectedLight.getValue().setLocked(newValue);
            settingsController.setDisabled(newValue | selectedLight.getValue().getGroupLocked());
            if (newValue)
            {
                selectedLight.getValue().setName("(X)");
            }
            else
            {
                selectedLight.getValue().setName("X");
            }
            tableView.refresh();
        }
    }

    @FXML
    private void deleteLight()
    {
        if (getSelected() != null)
        {
            getSelected().removeLight(lastSelectedIndex);
            tableView.refresh();
            tableView.getSelectionModel().selectPrevious();
        }
    }

    private int getRow()
    {
        return tableView.getSelectionModel().getSelectedIndex();
    }

    private LightGroupSetting getSelected()
    {
        return tableView.getSelectionModel().getSelectedItem();
    }
}