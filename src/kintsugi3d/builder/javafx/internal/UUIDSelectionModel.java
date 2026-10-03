/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.internal;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.LinkedHashMap;
import java.util.UUID;

public class UUIDSelectionModel
{
    private final ObservableMap<UUID, Boolean> selected =
        FXCollections.observableMap(new LinkedHashMap<>(16, 0.75f, true));

    private final ObjectProperty<UUID> lastSelected = new SimpleObjectProperty<>();

    // needs to be here to not get garbage-collected
    private final ObservableMap<UUID, Boolean> unmodifiableSelected = FXCollections.unmodifiableObservableMap(selected);

    public boolean isEmpty()
    {
        return selected.isEmpty();
    }

    public ObservableMap<UUID, Boolean> getSelected()
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return unmodifiableSelected;
    }

    public boolean isSelected(UUID id)
    {
        return selected.containsKey(id);
    }

    public BooleanExpression createSelectedBinding(UUID id)
    {
        return Bindings.createBooleanBinding(
            () -> selected.containsKey(id),
            this.selected
        );
    }

    public UUID getLastSelected()
    {
        return lastSelected.get();
    }

    public ObjectExpression<UUID> lastSelectedProperty()
    {
        return lastSelected;
    }

    public void select(UUID id)
    {
        selected.put(id, true);
        lastSelected.setValue(id);
    }

    public void unselect(UUID id)
    {
        selected.remove(id);

        // Gets the last item in the map -- O(n), unfortunately.
        lastSelected.setValue(selected.keySet().stream().reduce((a, b) -> b).orElse(null));
    }

    public void clearSelection()
    {
        selected.clear();
        lastSelected.setValue(null);
    }
}
