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
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.Iterator;

public class SearchableTreeView {
    private TreeView<String> treeView = new TreeView<>();
    private TreeItem<String> backupRoot; //use this to restore chunkTreeView to original state
    private TextInputControl searchTextInput;

    /**
     * Creates a tree view which can be "pruned" using a search term. Reverts back to the full tree when search term is "".
     * Inputted parameters are shallow copied into this object's member vars.
     * @param tree
     * @param txtInput
     */
    public SearchableTreeView(TreeView<String> tree, TextInputControl txtInput){
        backupRoot = deepCopy(tree.getRoot());

        treeView = tree;
        searchTextInput = txtInput;

        searchTextInput.textProperty().addListener((obs, oldVal, newVal)->{
            ObservableList<TreeItem<String>> leaves = getTreeViewLeaves();

            treeView.setRoot(deepCopy(backupRoot));

            String searchTxt = newVal.trim();
            if (searchTxt.isBlank()){
                treeView.setRoot(deepCopy(backupRoot));
                treeView.getRoot().setExpanded(true);
                return;
            }

            FilteredList<TreeItem<String>> filteredLeaves = new FilteredList<>(leaves, visibility->true);
            filteredLeaves.setPredicate(leaf->leaf.getValue().matches(".*" + searchTxt + ".*"));

            for (TreeItem<String> leaf : leaves){
                if(!filteredLeaves.contains(leaf)){
                    removeLeaf(treeView, leaf);
                }
            }
            treeView.getRoot().setExpanded(true);
        });
    }

    private void removeLeaf(TreeView<String> treeView, TreeItem<String> toRemove) {
        removeLeafRec(treeView.getRoot().getChildren().iterator(), toRemove);
    }

    private void removeLeafRec(Iterator<TreeItem<String>> curr, TreeItem<String> toRemove) {
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

    TreeItem<String> deepCopy(TreeItem<String> item) {
        TreeItem<String> copy = new TreeItem<>(item.getValue(), item.getGraphic());
        for (TreeItem<String> child : item.getChildren()) {
            copy.getChildren().add(deepCopy(child));
        }
        return copy;
    }

    private ObservableList<TreeItem<String>> getTreeViewLeaves() {
        ObservableList<TreeItem<String>> list = FXCollections.observableArrayList();
        getTreeViewLeavesRec(backupRoot, list);
        return list;
    }

    private void getTreeViewLeavesRec(TreeItem<String> item, ObservableList<TreeItem<String>> list) {
        if(item.isLeaf()){
            list.add(item);
        }
        else{
            for (TreeItem<String> child : item.getChildren()){
                getTreeViewLeavesRec(child, list);
            }
        }
    }
}
