/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.PTMfit;

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
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class PTMrequestUI implements IBRRequestUI {

    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportDirectoryField;
    @FXML private Button runButton;
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private IBRelightModels modelAccess;
    private Stage stage;

    private File lastDirectory;




    public static PTMrequestUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/PTMrequestUI.fxml";
        URL url = PTMrequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;
        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        PTMrequestUI svdRequestUI = fxmlLoader.getController();
        svdRequestUI.modelAccess = modelAccess;

        svdRequestUI.stage = new Stage();
        svdRequestUI.stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        svdRequestUI.stage.setTitle("PTM fit request");
        svdRequestUI.stage.setScene(new Scene(parent));
        svdRequestUI.stage.initOwner(window);

        return svdRequestUI;

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
    public <ContextType extends Context<ContextType>> void prompt(IBRRequestQueue<ContextType> requestQueue) {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            IBRRequest<ContextType> request = new PTMrequest<>(new TextureFitSettings(
                    Integer.parseInt(widthTextField.getText()),
                    Integer.parseInt(heightTextField.getText()),
                    new File(exportDirectoryField.getText()),
                    modelAccess.getSettingsModel()
                    ));

            requestQueue.addIBRRequest(request);
        });
    }
}
