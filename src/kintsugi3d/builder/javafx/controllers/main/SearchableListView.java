/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.main;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class SearchableListView extends SearchableView {
    ListView<String> listView;
    List<String> backup;
    public SearchableListView(ListView<String> viewList, TextField searchTxtField, CheckBox regexMode) {
        this.listView = viewList;
        this.textInput = searchTxtField;
        this.regexMode = regexMode;
    }

    public static SearchableListView createUnboundInstance(ListView<String> viewList, TextField searchTxtField) {
        return new SearchableListView(viewList, searchTxtField, null);
    }

    private static SearchableListView createUnboundInstance(ListView<String> viewList, TextField searchTxtField, CheckBox regexMode) {
        return new SearchableListView(viewList, searchTxtField, regexMode);
    }

    @Override
    public SearchableView bind() {
        backup = new ArrayList<>();
        backup.addAll(listView.getItems());

        textInput.textProperty().addListener((obs, oldText, newText)-> updateView());

        if(regexMode != null){
            regexMode.selectedProperty().addListener((obs, oldVal, newVal)-> updateView());
        }
        return this;
    }

    @Override
    protected void updateView() {
        listView.getItems().clear();
        listView.getItems().addAll(backup);

        String searchTxt = textInput.getText().trim();
        if (searchTxt.isBlank()){
            return;
        }

        FilteredList<String> filteredItems = new FilteredList<>(listView.getItems(), visibility->true);

        filteredItems.setPredicate(regexMode != null && regexMode.isSelected() ?
                item->item.matches(".*" + searchTxt + ".*") :
                item->item.contains(searchTxt));


        for (String item : backup) {
            if (!filteredItems.contains(item)) {
                listView.getItems().remove(item);
            }
        }

    }
}
