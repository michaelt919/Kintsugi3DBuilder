package kintsugi3d.builder.javafx.internal;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.SingleSelectionModel;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.*;

public class CardsModelImpl implements CardsModel {
    private String label;
    private SingleSelectionModel<String> selectedCameraViewModel;
    private ObservableList<ProjectDataCard> cameraCardsList;

    public CardsModelImpl(String label) {
        this.label = label;
        List<ProjectDataCard> dummyCards = new ArrayList<>();
        dummyCards.add(new ProjectDataCard("Card One", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<String, String>() {{
            put("File Name", "file_one"); put("Resolution", "320x200"); put("Size", "500 KB"); put("Purpose", "This is a description pertaining to the FIRST card."); put("Labels", "");
        }}));

        dummyCards.add(new ProjectDataCard("Card Two", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<String, String>() {{
            put("File Name", "file_two"); put("Resolution", "1080x200"); put("Size", "1000 KB"); put("Purpose", "This is a description pertaining to the SECOND card."); put("Labels", "");
        }}));

        dummyCards.add(new ProjectDataCard("Card Three", "@../../../../../../Kintsugi3D-icon.png", new LinkedHashMap<String, String>() {{
            put("File Name", "file_three"); put("Resolution", "720x200"); put("Size", "1500 KB"); put("Purpose", "This is a description pertaining to the THIRD card."); put("Labels", "");
        }}));

        this.setCardsList(dummyCards);

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
    public ProjectDataCard getSelectedCard() { return cameraCardsList.get(selectedCameraViewModel.getSelectedIndex());}

    @Override
    public int getSelectedCardIndex()
    {
        return selectedCameraViewModel.getSelectedIndex();
    }

    @Override
    public void setSelectedCardIndex(int cameraIndex)
    {
        selectedCameraViewModel.select(cameraIndex);
    }

    @Override
    public ReadOnlyIntegerProperty getSelectedCardIndexProperty() { return selectedCameraViewModel.selectedIndexProperty(); }

    @Override
    public List<ProjectDataCard> getCardsList() {
        return Collections.unmodifiableList(cameraCardsList);
    }

    @Override
    public ObservableList<ProjectDataCard> getItems() {
        return cameraCardsList;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setCardsList(List<ProjectDataCard> cardsList) {
        cameraCardsList = new ObservableListWrapper<>(cardsList);
    }

    @Override
    public void deselectCard() {
        selectedCameraViewModel.clearSelection();
    }

    @Override
    public void replaceCard(int index) {

    }

    @Override
    public void deleteCard(int index) {
        cameraCardsList.remove(index);
    }
}
