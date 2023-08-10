/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.systemsettings;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SystemSettingsController {

    public AnchorPane settingsFxmlHost;//holds the fxml which contains whatever settings the user is modifying
    @FXML
    private ListView<String> settingsListView;

    static final String FOLDER_PATH = "src/main/resources/fxml/menubar/systemsettings";

    public void init() {
        populateFileList();
        //initialize listeners for cell items
        settingsListView.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            String selectedItem = settingsListView.getSelectionModel().getSelectedItem();

            //remove spaces from string and append ".fxml"
            String fileName = "/fxml/menubar/systemsettings/" + selectedItem.replace(" ", "") + ".fxml";

            Parent newContent = null;
            try {
                //TODO: VERIFY THAT THE USER HAS SAVED ALL NECESSARY SETTINGS BEFORE SWITCHING?
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fileName));
                newContent = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (newContent != null) {
                settingsFxmlHost.getChildren().setAll(newContent);
            }
        });
    }

    //fill the listView with the fxml files in String folderPath
    private void populateFileList() {
        File folder = new File(FOLDER_PATH);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                List<String> fileNames = Arrays.stream(files)
                        .filter(File::isFile)
                        .map(File::getName)
                        .collect(Collectors.toList());

                settingsListView.getItems().addAll(parseFileNames(fileNames));
            }
        }
    }

    private Collection<String> parseFileNames(List<String> fileNames) {
        Collection<String> newFileNames = new ArrayList<>();
        for(String fileName : fileNames){
            String parsedFileName = parseString(fileName);

            if(!parsedFileName.equals("System Settings")) {
                newFileNames.add(parsedFileName);
            }
        }

        return newFileNames;
    }

    private static String parseString(String input) {
        //ex. turn "AutosaveSettings.fmxl" into "Autosave Settings"
        StringBuilder parsedString = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                parsedString.append(" ");
            }
            parsedString.append(c);
        }

        return parsedString.toString().trim().substring(0, parsedString.length() - 6); //remove ".fxml";
    }
}

