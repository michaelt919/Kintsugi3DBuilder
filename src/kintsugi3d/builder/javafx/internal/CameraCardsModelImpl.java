package kintsugi3d.builder.javafx.internal;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SingleSelectionModel;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CameraCardsModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CameraCardsModelImpl implements CameraCardsModel {
    private SingleSelectionModel<String> selectedCameraViewModel;
    private ObservableList<ProjectDataCard> cameraCardsList;

    public CameraCardsModelImpl() {
        List<ProjectDataCard> dummyCards = new ArrayList<>();
        dummyCards.add(new ProjectDataCard("cardone", "first_file_name", "200x200", "500 KB", "This is a description pertaining to the first card.", null, "somepath"));
        dummyCards.add(new ProjectDataCard("cardtwo", "second_file_name", "1080x200", "1000 KB", "This is a description pertaining to the second card.", null, "somepath"));
        dummyCards.add(new ProjectDataCard("cardthree", "third_file_name", "720x200", "1500 KB", "This is a description pertaining to the third card.", null, "somepath"));
        this.setCameraCardsList(dummyCards);

        selectedCameraViewModel = new SingleSelectionModel<>() {
            @Override
            protected String getModelItem(int index) {
                return cameraCardsList.get(index).getHeaderName();
            }

            @Override
            protected int getItemCount() {
                return cameraCardsList.size();
            }
        };
    }

    @Override
    public String getSelectedCameraView() { return selectedCameraViewModel.getSelectedItem();}

    @Override
    public int getSelectedCameraViewIndex()
    {
        return selectedCameraViewModel.getSelectedIndex();
    }

    @Override
    public void setSelectedCameraViewIndex(int cameraIndex)
    {
        selectedCameraViewModel.select(cameraIndex);
    }

    @Override
    public ReadOnlyIntegerProperty getSelectedCameraViewIndexProperty() { return selectedCameraViewModel.selectedIndexProperty(); }

    @Override
    public List<ProjectDataCard> getCameraCardsList() {
        return Collections.unmodifiableList(cameraCardsList);
    }

    @Override
    public ObservableList<ProjectDataCard> getItems() {
        return cameraCardsList;
    }

    @Override
    public void setCameraCardsList(List<ProjectDataCard> cameraViewList) {
        cameraCardsList = new ObservableListWrapper<>(cameraViewList);
    }

    @Override
    public void deselectCard() {
        selectedCameraViewModel.clearSelection();
    }

    @Override
    public void replaceCamera(int index) {

    }

    @Override
    public void refreshCamera(int index) {

    }

    @Override
    public void disableCamera(int index) {

    }

    @Override
    public void deleteCamera(int index) {
        cameraCardsList.remove(index);
    }
}
