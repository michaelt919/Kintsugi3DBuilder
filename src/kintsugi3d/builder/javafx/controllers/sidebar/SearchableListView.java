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

package kintsugi3d.builder.javafx.controllers.sidebar;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class SearchableListView<T> extends SearchableView
{
    private final ListView<T> listView;
    private List<T> backup;

    public SearchableListView(ListView<T> viewList, TextField searchTxtField, CheckBox regexMode)
    {
        this.listView = viewList;
        this.textInput = searchTxtField;
        this.regexMode = regexMode;
    }

    public static <T> SearchableListView<T> createUnboundInstance(ListView<T> viewList, TextField searchTxtField)
    {
        return new SearchableListView<>(viewList, searchTxtField, null);
    }

    private static <T> SearchableListView<T> createUnboundInstance(ListView<T> viewList, TextField searchTxtField, CheckBox regexMode)
    {
        return new SearchableListView<>(viewList, searchTxtField, regexMode);
    }

    @Override
    public SearchableView bind()
    {
        backup = new ArrayList<>(listView.getItems());

        textInput.textProperty().addListener((obs, oldText, newText)-> updateView());

        if(regexMode != null)
        {
            regexMode.selectedProperty().addListener((obs, oldVal, newVal)-> updateView());
        }
        return this;
    }

    @Override
    protected void updateView() {

        listView.getItems().clear();
        listView.getItems().addAll(backup);

        String searchTxt = textInput.getText().trim();
        if (searchTxt.isBlank())
        {
            return;
        }

        FilteredList<T> filteredItems = new FilteredList<>(listView.getItems(), visibility->true);

        filteredItems.setPredicate(item ->
        {
            String itemName = item.toString();

            if (regexMode != null && regexMode.isSelected())
            {
                return itemName.matches(String.format(".*%s.*", searchTxt));
            }
            else
            {
                return itemName.contains(searchTxt);
            }
        });


        for (T item : backup)
        {
            if (!filteredItems.contains(item))
            {
                listView.getItems().remove(item);
            }
        }

    }
}
