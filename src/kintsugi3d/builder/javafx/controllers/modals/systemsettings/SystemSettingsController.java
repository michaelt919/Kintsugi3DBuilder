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

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.core.JavaFXState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SystemSettingsController
{
    private static final Logger LOG = LoggerFactory.getLogger(SystemSettingsController.class);

    @FXML private Pane settingsFxmlHost;//holds the fxml which contains whatever settings the user is modifying
    //TODO: NEED TO REAPPLY ALL DEFAULT CHECKBOX SETTINGS
    @FXML private ListView<String> settingsListView;

    //Note: settings string MUST MATCH their .fxml counterparts
    //ex. Autosave Settings --> AutosaveSettings.fxml
    //    System Memory --> SysMem.fxml will not work
    private static final Map<String, String> SETTINGS_PAGES =
        Collections.unmodifiableMap(
            Stream.of(
                Map.entry("Cache Settings", "/fxml/modals/systemsettings/CacheSettings.fxml"),
                Map.entry("Lighting Settings", "/fxml/modals/systemsettings/LightingSettings.fxml"),
                Map.entry("Photo Projection Settings", "/fxml/modals/systemsettings/PhotoProjectionSettings.fxml"),
                Map.entry("System Memory Settings", "/fxml/modals/systemsettings/SystemMemorySettings.fxml"),
                Map.entry("Visual Settings", "/fxml/modals/systemsettings/VisualSettings.fxml")
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> u, LinkedHashMap::new)));

    public void initializeSettingsPages(Window parentWindow, JavaFXState state)
    {
        populateFileList();
        //initialize listeners for cell items
        settingsListView.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) ->
        {
            //remove spaces from string and append ".fxml"
            String fileName = SETTINGS_PAGES.get(observableValue.getValue());

            Parent newContent = null;
            try
            {
                //TODO: VERIFY THAT THE USER HAS SAVED ALL NECESSARY SETTINGS BEFORE SWITCHING?
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fileName));
                newContent = loader.load();

                //initialize controller
                SystemSettingsControllerBase controller = loader.getController();
                controller.initializeSettingsPage(parentWindow, state);
            }
            catch (IOException e)
            {
                LOG.error("Error occurred opening system settings.", e);
            }

            if (newContent != null)
            {
                settingsFxmlHost.getChildren().setAll(newContent);
            }
        });
        settingsListView.setFixedCellSize(40);
    }

    //fill the listView with the fxml files in String folderPath
    private void populateFileList()
    {
        settingsListView.getItems().clear();
        settingsListView.getItems().addAll(SETTINGS_PAGES.keySet());
    }
}

