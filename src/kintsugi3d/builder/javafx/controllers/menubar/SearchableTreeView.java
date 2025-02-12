/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.Iterator;

public final class SearchableTreeView extends SearchableView {
    TreeView<String> treeView;
    TreeItem<String> backupRoot;

    private SearchableTreeView(TreeView<String> tree, TextInputControl textInput, CheckBox regexMode){
        this.treeView = tree;
        this.textInput = textInput;
        this.regexMode = regexMode;
    }

    public SearchableTreeView bind() {
        backupRoot = deepCopy(treeView.getRoot());

        textInput.textProperty().addListener((obs, oldText, newText)-> updateView());

        if(regexMode != null){
            regexMode.selectedProperty().addListener((obs, oldVal, newVal)-> updateView());
        }
        return this;
    }

    public static SearchableTreeView createUnboundInstance(TreeView<String> tree, TextInputControl textInput, CheckBox regexMode){
        return new SearchableTreeView(tree, textInput, regexMode);
    }

    public static SearchableTreeView createUnboundInstance(TreeView<String> tree, TextInputControl textInput){
        return new SearchableTreeView(tree, textInput, null);
    }

    @Override
    protected void updateView() {
        treeView.setRoot(deepCopy(backupRoot));

        String searchTxt = textInput.getText().trim();
        if (searchTxt.isBlank()){
            treeView.getRoot().setExpanded(true);
            return;
        }

        ObservableList<TreeItem<String>> leaves = getTreeViewLeaves(backupRoot);
        FilteredList<TreeItem<String>> filteredLeaves = new FilteredList<>(leaves, visibility->true);

        filteredLeaves.setPredicate(regexMode != null && regexMode.isSelected() ? leaf->leaf.getValue().matches(".*" + searchTxt + ".*") :
                leaf->leaf.getValue().contains(searchTxt));


        for (TreeItem<String> leaf : leaves){
            if(!filteredLeaves.contains(leaf)){
                removeLeaf(treeView, leaf);
            }
        }

        for (TreeItem<String> group : getTreeViewGroups(treeView.getRoot())){
            group.setExpanded(true);
        }
    }

    private static void removeLeaf(TreeView<String> treeView, TreeItem<String> toRemove) {
        removeLeafRec(treeView.getRoot().getChildren().iterator(), toRemove);
    }

    private static void removeLeafRec(Iterator<TreeItem<String>> curr, TreeItem<String> toRemove) {
        TreeItem<String> currItem = curr.next();

        if(currItem.getValue().equals(toRemove.getValue())){
            curr.remove();
            return;
        }

        //search child nodes if needed
        if(!currItem.getChildren().isEmpty()){
            Iterator<TreeItem<String>> childrenIter = currItem.getChildren().iterator();
            removeLeafRec(childrenIter, toRemove);
        }

        //search sibling nodes if needed
        if(curr.hasNext()){
            removeLeafRec(curr, toRemove);
        }
    }

    private static TreeItem<String> deepCopy(TreeItem<String> item) {
        TreeItem<String> copy = new TreeItem<>(item.getValue(), item.getGraphic());
        for (TreeItem<String> child : item.getChildren()) {
            copy.getChildren().add(deepCopy(child));
        }
        return copy;
    }

    private static ObservableList<TreeItem<String>> getTreeViewGroups(TreeItem<String> backupRoot){
        ObservableList<TreeItem<String>> list = FXCollections.observableArrayList();
        getTreeViewGroupsRec(backupRoot, list);
        return list;
    }

    private static void getTreeViewGroupsRec(TreeItem<String> item, ObservableList<TreeItem<String>> list) {
        if (!item.getChildren().isEmpty()){
            list.add(item);

            for(TreeItem<String> child : item.getChildren()){
                getTreeViewGroupsRec(child, list);
            }
        }
    }

    private static ObservableList<TreeItem<String>> getTreeViewLeaves(TreeItem<String> backupRoot) {
        ObservableList<TreeItem<String>> list = FXCollections.observableArrayList();
        getTreeViewLeavesRec(backupRoot, list);
        return list;
    }

    private static void getTreeViewLeavesRec(TreeItem<String> item, ObservableList<TreeItem<String>> list) {
        if(item.isLeaf()){
            list.add(item);
        }
        else{
            for (TreeItem<String> child : item.getChildren()){
                getTreeViewLeavesRec(child, list);
            }
        }
    }

    public TreeView<String> getTreeView() {
        return this.treeView;
    }
}
