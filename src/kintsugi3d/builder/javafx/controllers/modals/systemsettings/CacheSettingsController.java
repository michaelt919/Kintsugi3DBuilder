/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.core.JavaFXState;
import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;
import kintsugi3d.builder.javafx.util.SafeFloatStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class CacheSettingsController implements SystemSettingsControllerBase
{
    @FXML private CheckBox sizeCheck;
    @FXML private CheckBox recentCheck;
    @FXML private CheckBox timeCheck;
    @FXML private TextField numGB;
    @FXML private TextField numRecent;
    @FXML private TextField numDays;
    @FXML private Label previewImageCacheLabel;
    @FXML private Label specularFitCacheLabel;
    @FXML private Label cacheSizeLabel;
    @FXML private Button cleanCacheButton;

    private static final Logger LOG = LoggerFactory.getLogger(CacheSettingsController.class);

    @Override
    public void initializePage(Window parentWindow, JavaFXState state)
    {
        previewImageCacheLabel.setText(ApplicationFolders.getPreviewImagesRootDirectory().toString());
        specularFitCacheLabel.setText(ApplicationFolders.getFitCacheRootDirectory().toString());

        StringBinding cacheSizeTextBase = Bindings.createStringBinding(
            () -> String.format("Cache Size: %.2fGB", Global.state().getCacheModel().getCacheSizeGB()), Global.state().getCacheModel().getCacheSizeGBProperty());

        // Three cases for cache size label:
        // 1. Cache size previously calculated
        // 2. Cache size previously calculated but being recalculated
        // 3. Cache size not yet calculated; calculation should be in progress.
        cacheSizeLabel.textProperty().bind(Bindings.when(Global.state().getCacheModel().getCacheSizeGBProperty().greaterThanOrEqualTo(0.0))
            .then(Bindings.when(Global.state().getCacheModel().getCacheSizeCalcInProgressProperty())
                .then(cacheSizeTextBase.concat(" (calculating...)"))
                .otherwise(cacheSizeTextBase))
            .otherwise(new ReadOnlyStringWrapper("Cache Size: (calculating...)")));

        // Request a refresh of the cache size without an explicit callback.
        Global.state().getCacheModel().requestCacheSizeRefresh();

        bind(state.getSettingsModel());
    }

    public void bind(ObservableGeneralSettingsModel injectedSettingsModel)
    {
        sizeCheck.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("sizePromptEnabled"));
        recentCheck.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("recentPromptEnabled"));
        timeCheck.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("fileAgePromptEnabled"));

        numGB.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("cacheSizeLimit"),
            new SafeFloatStringConverter(32.0f));
        numRecent.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("recentProjectLimit"),
            new SafeNumberStringConverter(5));
        numDays.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("fileAgeLimit"),
            new SafeNumberStringConverter(30));
    }

    @FXML private void openDirectory(MouseEvent e){
        if (!(e.getSource() instanceof Label)) {
            return;
        }

        Label label = (Label) e.getSource();
        File file = new File(label.getText());
        if (!file.exists()){
            ButtonType ok = new ButtonType("OK", ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(AlertType.NONE, String.format("Cache path not found: %s", label.getText()), ok);

            alert.setTitle("Cache path not found");
            alert.show();
            return;
        }

        try{
            Desktop.getDesktop().open(file);
        }
        catch(IOException ioe){
            ExceptionHandling.error("Failed to open project directory", ioe);
        }
    }

    @FXML private void clearCache()
    {
        Global.state().getCacheModel().clearCache();
    }

    private static void handleCacheCleanupError(IOException e)
    {
        LOG.error(e.toString());
        ExceptionHandling.error("An error occurred while cleaning up cache.  Consider deleting cache files manually.", e);
    }


    @FXML private void cleanUpCacheButton()
    {
        Global.state().getCacheModel().cleanUpCache();
    }


//    // Not using this since it scares me.
//    private void deleteRecursively(File file)
//    {
//        deleteRecursively(file, file);
//    }
//
//    private void deleteRecursively(File original, File current)
//    {
//        if (current.isDirectory())
//        {
//            File[] contents = current.listFiles();
//            if (contents != null)
//            {
//                for (File f : contents)
//                {
//                    deleteRecursively(original, f);
//                }
//            }
//
//            // Extra check due to danger of this operation
//            assert current.toString().startsWith(original.toString());
//            current.delete();
//        }
//        else
//        {
//            // Extra check due to danger of this operation
//            assert current.toString().startsWith(original.toString());
//            current.delete();
//        }
//    }
}