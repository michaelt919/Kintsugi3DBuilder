package kintsugi3d.builder.util;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import kintsugi3d.builder.resources.ProjectDataCard;

import java.util.LinkedHashSet;
import java.util.List;

public class CardSelectionModel {
    private ObservableSet<String> selectedCards = FXCollections.observableSet(new LinkedHashSet<>());
    private final Property<ObservableList<ProjectDataCard>> allCards;
    private StringProperty lastSelected = new SimpleStringProperty();

    public CardSelectionModel(Property<ObservableList<ProjectDataCard>> listProperty) {
        allCards = listProperty;
    }

    public void rebindCardList(Property<ObservableList<ProjectDataCard>> listProperty) {
        allCards.bind(listProperty);
    }

    public ObservableSet<String> getSelectedIds() {
        return selectedCards;
    }

    public ObservableList<ProjectDataCard> getSelectedItems() {
        return FXCollections.observableList(allCards.getValue().filtered(card-> selectedCards.contains(card.getCardId())));
    }

    public void selectAll() {
        for (ProjectDataCard card : allCards.getValue()) {
            selectedCards.add(card.getCardId());
        }
    }

    public void clearAndSelect(String id) {
        selectedCards.clear();
        selectedCards.add(id);
        lastSelected.setValue(id);
    }

    public void select(String id) {
        selectedCards.add(id);
        lastSelected.setValue(id);
    }

    public void select(List<ProjectDataCard> cards) {
        for(ProjectDataCard card : cards) {
            selectedCards.add(card.getCardId());
        }
    }

    public void clearSelection(String id) {
        selectedCards.remove(id);
        lastSelected.setValue(null);
    }

    public void clearSelection() {
        selectedCards.clear();
        lastSelected.setValue(null);
    }

    public boolean isSelected(String id) {
        return selectedCards.contains(id);
    }

    public BooleanBinding isSelectedProperty(String id) {
        return Bindings.createBooleanBinding(
                () -> selectedCards.contains(id),
                selectedCards
        );
    }

    public boolean isEmpty() {
        return selectedCards.isEmpty();
    }

    public String getLastSelectedValue() {
        return lastSelected.getValue();
    }

    public StringProperty getLastSelectedProperty() {
        return lastSelected;
    }
}
