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

package kintsugi3d.builder.export.screenshot;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.GraphicsRequestController;
import kintsugi3d.builder.core.GraphicsRequestQueue;
import kintsugi3d.builder.export.screenshot.ScreenshotRequest.Builder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.util.RecentProjects;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ScreenshotController implements GraphicsRequestController
{
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportFileField;
    @FXML private Button runButton;

    private final FileChooser fileChooser = new FileChooser();
    public interface BuilderSupplier
    {
        Builder<ScreenshotRequest> get();
    }

    private BuilderSupplier builderSupplier;

    private Stage stage;

    public static ScreenshotController create(Window window) throws IOException
    {
        String fxmlFileName = "fxml/modals/export/ScreenshotUI.fxml";
        URL url = ScreenshotController.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        ScreenshotController screenshotUI = fxmlLoader.getController();

        screenshotUI.stage = new Stage();
        screenshotUI.stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        screenshotUI.stage.setTitle("Screenshot request");
        screenshotUI.stage.setScene(new Scene(parent));
        screenshotUI.stage.initOwner(window);

        screenshotUI.fileChooser.getExtensionFilters().removeAll();
        screenshotUI.fileChooser.getExtensionFilters().add(new ExtensionFilter("Image files", "png", "jpeg", "jpg"));

        return screenshotUI;
    }

    public void setBuilderSupplier(BuilderSupplier builderSupplier)
    {
        this.builderSupplier = builderSupplier;
    }

    @FXML
    private void exportFileButtonAction()
    {
        this.fileChooser.setTitle("Choose an export file");
        if (exportFileField.getText().isEmpty())
        {
            this.fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
        else
        {
            File currentValue = new File(exportFileField.getText());
            this.fileChooser.setInitialDirectory(currentValue);
        }
        File file = this.fileChooser.showSaveDialog(stage.getOwner());
        if (file != null)
        {
            exportFileField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file);
        }
    }

    @FXML
    public void cancelButtonAction(ActionEvent actionEvent)
    {
        stage.close();
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(GraphicsRequestQueue<ContextType> requestQueue)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();
            if (builderSupplier != null)
            {
                if(Global.state().getIOModel().getProgressMonitor().isConflictingProcess()){
                    return;
                }

                requestQueue.addGraphicsRequest(
                    builderSupplier.get()
                        .setWidth(Integer.parseInt(widthTextField.getText()))
                        .setHeight(Integer.parseInt(heightTextField.getText()))
                        .setExportFile(new File(exportFileField.getText()))
                        .create());
            }
        });
    }
}
