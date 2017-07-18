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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

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
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        tableView.getColumns().add(nameCol);

        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
            final Integer tempFinalInt = i;

            TableColumn<LightGroupSetting, LightSetting> newCol = new TableColumn<>("L" + (tempFinalInt+1));

            newCol.setCellValueFactory(param -> param.getValue().lightListProperty().valueAt(tempFinalInt));

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
                    if(selected != null){
                        if(selected.getValue() instanceof LightSetting){
                            selectedLight.setValue((LightSetting) selected.getValue());
                            lastSelectedIndex = tb.getColumn()-1;
                        }
                        else if(selected.getValue() instanceof String){
                            selectedLight.setValue(null);
                            lastSelectedIndex = -1;
                        }
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

        lightGroups.add(new LightGroupSetting("Free Lights"));


        selectedLight.addListener(settingsController.changeListener);



    }

    @FXML private void newGroup(){
        System.out.println("TODO newGroupButton");//TODO
    }
    @FXML private void saveGroup(){
        System.out.println("TODO saveGroup");//TODO
    }
    @FXML private void renameGroup(){
        System.out.println("TODO renameGroup");//TODO
    }
    @FXML private void moveUPGroup(){
        System.out.println("TODO moveUPGroup");//TODO
    }
    @FXML private void moveDOWNGroup(){
        System.out.println("TODO moveDOWNGroup");//TODO
    }
    @FXML private void lockGroup(){
        System.out.println("TODO lockGroup");//TODO
    }
    @FXML private void keyframeGroup(){
        System.out.println("TODO keyframeGroup");//TODO
    }
    @FXML private void deleteGroup(){
        System.out.println("TODO deleteGroup");//TODO
    }
    @FXML private void newLight(){
        System.out.println("TODO newLight");//TODO
    }
    @FXML private void saveLight(){
        System.out.println("TODO saveLight");//TODO
    }
    @FXML private void lockLight(){
        System.out.println("TODO lockLight");//TODO
    }
    @FXML private void deleteLight(){
        System.out.println("TODO deleteLight");//TODO
    }




}
