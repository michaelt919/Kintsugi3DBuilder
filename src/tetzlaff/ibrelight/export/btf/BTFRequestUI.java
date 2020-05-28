/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.btf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.IBRRequestUI;
import tetzlaff.ibrelight.core.IBRelightModels;

public class BTFRequestUI implements IBRRequestUI
{
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportDirectoryField;
    @FXML private TextField viewIndicesTextField;
    @FXML private Button runButton;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    private IBRelightModels modelAccess;
    private Stage stage;

    private File lastDirectory;

    public static BTFRequestUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/BTFRequestUI.fxml";
        URL url = BTFRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        BTFRequestUI btfRequestUI = fxmlLoader.getController();
        btfRequestUI.modelAccess = modelAccess;

        btfRequestUI.stage = new Stage();
        btfRequestUI.stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        btfRequestUI.stage.setTitle("BTF request");
        btfRequestUI.stage.setScene(new Scene(parent));
        btfRequestUI.stage.initOwner(window);

        return btfRequestUI;
    }

    @FXML
    private void exportDirectoryButtonAction()
    {
        this.directoryChooser.setTitle("Choose an export directory");
        if (exportDirectoryField.getText().isEmpty())
        {
            if (lastDirectory != null)
            {
                this.directoryChooser.setInitialDirectory(lastDirectory);
            }
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
            lastDirectory = file;
        }
    }

    @FXML
    public void cancelButtonAction(ActionEvent actionEvent)
    {
        stage.close();
    }

    @Override
    public void prompt(Consumer<IBRRequest> requestHandler)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            BTFRequest request = new BTFRequest(
                Integer.parseInt(widthTextField.getText()),
                Integer.parseInt(heightTextField.getText()),
                new File(exportDirectoryField.getText()),
                modelAccess.getSettingsModel(),
                modelAccess.getLightingModel().getLightPrototype(0).getColor());

            String[] viewIndexStrings = viewIndicesTextField.getText().split("\\s*,\\s*");

            if (viewIndexStrings.length > 0)
            {
                List<Integer> viewIndices = new ArrayList<>(viewIndexStrings.length);

                for (String str : viewIndexStrings)
                {
                    if (!str.trim().isEmpty())
                    {
                        try
                        {
                            viewIndices.add(Integer.parseInt(str));
                        }
                        catch(NumberFormatException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

                if (!viewIndices.isEmpty())
                {
                    request.setViewIndices(viewIndices);
                }
            }

            requestHandler.accept(request);
        });
    }
}
