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

package kintsugi3d.builder.export.simpleanimation;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.IBRRequestQueue;
import kintsugi3d.builder.core.IBRRequestUI;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.export.simpleanimation.SimpleAnimationRequestBase.Builder;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.gl.core.Context;
import kintsugi3d.util.RecentProjects;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Supplier;

public class SimpleAnimationUI implements IBRRequestUI
{
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportDirectoryField;
    @FXML private TextField frameCountTextField;
    @FXML private Button runButton;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    private Supplier<Builder> builderSupplier;

    private Stage stage;

    public static SimpleAnimationUI create(Window window, Kintsugi3DBuilderState modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/SimpleAnimationUI.fxml";
        URL url = SimpleAnimationUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        SimpleAnimationUI simpleAnimationUI = fxmlLoader.getController();

        simpleAnimationUI.stage = new Stage();
        simpleAnimationUI.stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        simpleAnimationUI.stage.setTitle("Animation request");
        simpleAnimationUI.stage.setScene(new Scene(parent));
        simpleAnimationUI.stage.initOwner(window);

        return simpleAnimationUI;
    }

    public void setBuilderSupplier(Supplier<Builder> builderSupplier)
    {
        this.builderSupplier = builderSupplier;
    }

    @FXML
    private void exportDirectoryButtonAction()
    {
        this.directoryChooser.setTitle("Choose an export directory");
        if (exportDirectoryField.getText().isEmpty())
        {
            this.directoryChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
        else
        {
            File currentValue = new File(exportDirectoryField.getText());
            this.directoryChooser.setInitialDirectory(currentValue);
        }
        File file = this.directoryChooser.showDialog(stage.getOwner());
        if (file != null)
        {
            exportDirectoryField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file);
        }
    }

    @FXML
    public void cancelButtonAction(ActionEvent actionEvent)
    {
        stage.close();
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(IBRRequestQueue<ContextType> requestQueue)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();
            if (builderSupplier != null)
            {
                if(MultithreadModels.getInstance().getIOModel().getProgressMonitor().isConflictingProcess()){
                    return;
                }

                requestQueue.addIBRRequest(
                    builderSupplier.get()
                        .setWidth(Integer.parseInt(widthTextField.getText()))
                        .setHeight(Integer.parseInt(heightTextField.getText()))
                        .setFrameCount(Integer.parseInt(frameCountTextField.getText()))
                        .setExportPath(new File(exportDirectoryField.getText()))
                        .create());
            }
        });
    }
}
