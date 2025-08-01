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

package kintsugi3d.builder.javafx.controllers.menubar.systemsettings;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import kintsugi3d.builder.javafx.JavaFXState;
import kintsugi3d.builder.util.Launch4jConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.ResourceBundle;

public class SystemMemoryController implements Initializable, SystemSettingsControllerBase
{
    private static final Logger log = LoggerFactory.getLogger(SystemMemoryController.class);

    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 1048576;

    @FXML private CheckBox maxMemCheckbox;
    @FXML private Spinner<Integer> maxMemSpinner;
    @FXML private Button okButton;
    @FXML private Button applyButton;
    @FXML private AnchorPane root;

    private Launch4jConfiguration configuration;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        init();
    }


    public void button_Apply()
    {
        configuration.setEnableMaxMemory(maxMemCheckbox.isSelected());
        configuration.setMaxMemoryMb((Integer)maxMemSpinner.getValue());

        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        try
        {
            try
            {
                configuration.write();
            }
            catch (AccessDeniedException e)
            {
                log.warn("The current user does not have sufficient privileges to write to launch4j configuration file", e);
                log.warn("Attempting to write as superuser, this will prompt for UAC!");
                configuration.writeAsSuperuser();
            }
        }
        catch (IOException e)
        {
            log.error("Failed to write to launch4j configuration file", e);
            Alert alert = new Alert(Alert.AlertType.NONE, "", ok);
            alert.setTitle("Kintsugi 3D Builder");
            alert.setHeaderText("Writing failed");
            alert.setContentText("Kintsugi 3D Builder failed to write to the configuration file. Try restarting Kintsugi 3D Builder as administrator and try again.");
            alert.show();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ok);
        alert.setTitle("Kintsugi 3D Builder");
        alert.setHeaderText("Restart Required");
        alert.setContentText("A restart of Kintsugi 3D Builder is needed for changes to take effect.");
        alert.show();
    }


    @Override
    public void init() {
        try
        {
            configuration = Launch4jConfiguration.read();
        }
        catch (IOException e)
        {
            log.error("Failed to read jvm configuration:", e);
            log.error("Using default configuration");
            configuration = Launch4jConfiguration.empty();
        }

        maxMemCheckbox.setSelected(configuration.isEnableMaxMemory());
        maxMemSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        MIN_VALUE, MAX_VALUE, configuration.getMaxMemoryMb(), 1));
    }

    @Override
    public void bindInfo(JavaFXState javaFXState) {
        //TODO: imp.
    }
}
