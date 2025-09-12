package kintsugi3d.builder.javafx.internal;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.util.LinkedHashSet;
import java.util.UUID;

public class UUIDSelectionModel
{
    private final ObservableSet<UUID> selectedCards = FXCollections.observableSet(new LinkedHashSet<>());
    private final ObjectProperty<UUID> lastSelected;

    public UUIDSelectionModel()
    {
        lastSelected = new SimpleObjectProperty<>();
    }

    public void select(UUID id)
    {
        selectedCards.add(id);
        lastSelected.setValue(id);
    }

    public void clearSelection(UUID id)
    {
        selectedCards.remove(id);
        lastSelected.setValue(null);
    }

    public void clearSelection()
    {
        selectedCards.clear();
        lastSelected.setValue(null);
    }

    public boolean isSelected(UUID id)
    {
        return selectedCards.contains(id);
    }

    public BooleanBinding isSelectedProperty(UUID id)
    {
        return Bindings.createBooleanBinding(
            () -> selectedCards.contains(id),
            this.selectedCards
        );
    }

    public boolean isEmpty()
    {
        return selectedCards.isEmpty();
    }
}
