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
import java.util.UUID;

public class CardSelectionModel {
    private ObservableSet<UUID> selectedCards = FXCollections.observableSet(new LinkedHashSet<>());
    private ObservableList<ProjectDataCard> allCards;
    private ObjectProperty<UUID> lastSelected;

    public CardSelectionModel(ObservableList<ProjectDataCard> list) {
        allCards = list;
        lastSelected = new SimpleObjectProperty<>();
    }

    public void setCardsList(ObservableList<ProjectDataCard> list) {
        allCards = list;
        selectedCards.clear();
    }

    public ObservableSet<UUID> getSelectedIds() {
        return selectedCards;
    }

    public ObservableList<ProjectDataCard> getSelectedItems() {
        return FXCollections.observableList(allCards.filtered(card-> selectedCards.contains(card.getCardId())));
    }

    public void selectAll() {
        for (ProjectDataCard card : allCards) {
            selectedCards.add(card.getCardId());
        }
    }

    public void clearAndSelect(UUID id) {
        selectedCards.clear();
        selectedCards.add(id);
        lastSelected.setValue(id);
    }

    public void select(UUID id) {
        selectedCards.add(id);
        lastSelected.setValue(id);
    }

    public void select(List<ProjectDataCard> cards) {
        for(ProjectDataCard card : cards) {
            selectedCards.add(card.getCardId());
        }
    }

    public void clearSelection(UUID id) {
        selectedCards.remove(id);
        lastSelected.setValue(null);
    }

    public void clearSelection() {
        selectedCards.clear();
        lastSelected.setValue(null);
    }

    public boolean isSelected(UUID id) {
        return selectedCards.contains(id);
    }

    public BooleanBinding isSelectedProperty(UUID id) {
        return Bindings.createBooleanBinding(
                () -> selectedCards.contains(id),
                this.selectedCards
        );
    }

    public boolean isEmpty() {
        return selectedCards.isEmpty();
    }

    public UUID getLastSelectedValue() {
        return lastSelected.getValue();
    }

    public ObjectProperty<UUID> getLastSelectedProperty() {
        return lastSelected;
    }
}
