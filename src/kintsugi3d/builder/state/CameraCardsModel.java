package kintsugi3d.builder.state;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;
import kintsugi3d.builder.resources.ProjectDataCard;

import java.util.List;

public interface CameraCardsModel {
    String getSelectedCameraView();
    int getSelectedCameraViewIndex();
    void setSelectedCameraViewIndex(int cameraViewIndex);
    ReadOnlyIntegerProperty getSelectedCameraViewIndexProperty();
    List<ProjectDataCard> getCameraCardsList();
    ObservableList<ProjectDataCard> getItems();
    void setCameraCardsList(List<ProjectDataCard> cameraViewList);
    void deselectCard();
    void replaceCamera(int index);
    void refreshCamera(int index);
    void disableCamera(int index);
    void deleteCamera(int index);
}
