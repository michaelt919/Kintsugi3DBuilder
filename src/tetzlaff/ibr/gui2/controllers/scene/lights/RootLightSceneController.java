package tetzlaff.ibr.gui2.controllers.scene.lights;//Created by alexk on 7/16/2017.

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.scene.control.skin.TableHeaderRow;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.ListExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.*;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.binding.Binding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class RootLightSceneController implements Initializable {
    @FXML private VBox settings;
    @FXML private SettingsLightSceneController settingsController;
    @FXML private TableView<LightGroupSetting> tableView;
    @FXML private VBox groupControls;
    @FXML private VBox lightControls;
    @FXML private Button theRenameButton;

    private final ObservableList<LightGroupSetting> lightGroups = new ObservableListWrapper<>(new ArrayList<>());
    private final Property<LightSetting> selectedLight = new SimpleObjectProperty<>();
    private int lastSelectedIndex = -1;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("GLSC started!");

        //TABLE SET UP
        //columns
        TableColumn<LightGroupSetting, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LightGroupSetting, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<LightGroupSetting, String> param) {
                String s = "";
                if(param.getValue().isLocked()) s = "(L)";
                return new SimpleStringProperty(s + param.getValue().getName());
            }
        });
        tableView.getColumns().add(nameCol);

        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
            final Integer tempFinalInt = i;

            TableColumn<LightGroupSetting, LightSetting> newCol = new TableColumn<>("L" + (tempFinalInt+1));

            newCol.setCellValueFactory(param -> param.getValue().lightListProperty().valueAt(tempFinalInt));

            newCol.setPrefWidth(35);

            tableView.getColumns().add(newCol);
        }



        tableView.getSelectionModel().setCellSelectionEnabled(true);

        //light selection listener
        tableView.getSelectionModel().getSelectedCells().addListener((ListChangeListener<TablePosition>) c -> {
            while (c.next()){
                if(c.wasAdded()){
                    //new cell selected
                    assert c.getAddedSize() == 1;
                    TablePosition tb = c.getAddedSubList().get(0);
                    ObservableValue selected = tb.getTableColumn().getCellObservableValue(tb.getRow());
                    if(selected != null && selected.getValue() instanceof LightSetting){
                            selectedLight.setValue((LightSetting) selected.getValue());
                            lastSelectedIndex = tb.getColumn()-1;
                        }
                        else {
                            selectedLight.setValue(null);
                            lastSelectedIndex = -1;
                        }

                }
            }
        });

        //preventing reordering or rearranging
        tableView.skinProperty().addListener((obs, oldS, newS)->{
            final TableHeaderRow tableHeaderRow = (TableHeaderRow) tableView.lookup("TableHeaderRow");
            tableHeaderRow.reorderingProperty().addListener((p, o, n)->
            tableHeaderRow.setReordering(false));
        });

        tableView.setSortPolicy(param -> false);

        tableView.setColumnResizePolicy(param -> false);

        tableView.setItems(lightGroups);

        //TABLE SET UP DONE

        //lightGroups.add(new LightGroupSetting("Free Lights"));

        selectedLight.addListener(settingsController.changeListener);




    }

    @FXML private void newGroup(){
        //for now we will create a blank group
        //in the future we may want to duplicate the previous group instead
        LightGroupSetting newGroup = new LightGroupSetting("New Group");
        lightGroups.add(newGroup);
        tableView.getSelectionModel().select(lightGroups.size()-1, tableView.getColumns().get(0));
    }
    @FXML private void saveGroup(){
        System.out.println("TODO saveGroup");//TODO
    }
    @FXML private void renameGroup(){

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
        EventHandler<ActionEvent> finishRename = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                groupControls.getChildren().removeAll(renameTextField, cancelRenameButton);
                theRenameButton.setOnAction(oldOnAction);

                groupControls.getChildren().iterator().forEachRemaining(n -> {
                    n.setDisable(false);
                });

//                tableView.refresh();

                settings.setDisable(false);
            }
        };

        EventHandler<ActionEvent> doRename = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String newName = renameTextField.getText();
                if (!newName.equals("")) {
                    tableView.getSelectionModel().getSelectedItem().setName(newName);
                }

                finishRename.handle(event);
            }
        };


        //set the on actions
        theRenameButton.setOnAction(doRename);
        cancelRenameButton.setOnAction(finishRename);
        renameTextField.setOnAction(doRename);

        renameTextField.setText(tableView.getSelectionModel().getSelectedItem().getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();


    }
    @FXML private void moveUPGroup(){
        if(getRow() > 0) Collections.swap(lightGroups, getRow(), getRow()-1);
    }
    @FXML private void moveDOWNGroup(){
        if(getRow() < lightGroups.size()-1) Collections.swap(lightGroups, getRow(), getRow()+1);
    }
    @FXML private void lockGroup(){
        if(getSelected() != null){
            boolean n = !getSelected().isLocked();
            getSelected().setLocked(n);
            if(selectedLight.getValue() != null)settingsController.setDisabled(n | selectedLight.getValue().isLocked());
        }

        tableView.refresh();

    }
    @FXML private void keyframeGroup(){
        System.out.println("TODO keyframeGroup");//TODO
    }
    @FXML private void deleteGroup(){
        if(lightGroups.size()>getRow() & getRow()>=0)lightGroups.remove(getRow());
    }

    @FXML private void newLight(){
        if(getSelected() != null)getSelected().addLight(lastSelectedIndex);
    }
    @FXML private void saveLight(){
        System.out.println("TODO saveLight");//TODO
    }
    @FXML private void lockLight(){
        boolean newValue = !selectedLight.getValue().isLocked();
        if(selectedLight.getValue() != null) {
            selectedLight.getValue().setLocked(newValue);
            settingsController.setDisabled(newValue | selectedLight.getValue().getGroupLocked());
            if(newValue)selectedLight.getValue().setName("(X)");
            else selectedLight.getValue().setName("X");
            tableView.refresh();
        }

    }
    @FXML private void deleteLight(){
        if(getSelected() != null) {
            getSelected().removeLight(lastSelectedIndex);
            tableView.refresh();
            tableView.getSelectionModel().selectPrevious();
        }
    }

    private int getRow(){
        return tableView.getSelectionModel().getSelectedIndex();
    }

    private LightGroupSetting getSelected(){
        return tableView.getSelectionModel().getSelectedItem();
    }

}
