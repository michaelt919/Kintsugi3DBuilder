/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.IBRRequestUI;
import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.core.ViewSet;

public class SpecularFitRequestUI implements IBRRequestUI
{
    @FXML private CheckBox smithCheckBox;
    @FXML private CheckBox levenbergMarquardtCheckBox;
    @FXML private TextField unsuccessfulLMIterationsTextField;
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportDirectoryField;

    @FXML private TextField basisCountTextField;
    @FXML private TextField mfdResolutionTextField;
    @FXML private TextField convergenceToleranceTextField;
    @FXML private TextField specularSmoothnessTextField;
    @FXML private TextField metallicityTextField;
    @FXML private CheckBox normalRefinementCheckBox;
    @FXML private TextField minNormalDampingTextField;
    @FXML private TextField normalSmoothingIterationsTextField;

    @FXML private TextField reconstructionViewSetField;

    @FXML private Button runButton;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private final FileChooser fileChooser = new FileChooser();

    private IBRelightModels modelAccess;
    private Stage stage;

    private File lastDirectory;
    private File lastViewSet;

    public static SpecularFitRequestUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/SpecularFitRequestUI.fxml";
        URL url = SpecularFitRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        SpecularFitRequestUI svdRequestUI = fxmlLoader.getController();
        svdRequestUI.modelAccess = modelAccess;

        svdRequestUI.stage = new Stage();
        svdRequestUI.stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        svdRequestUI.stage.setTitle("Specular fit request");
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
    public void reconstructionViewSetButtonAction()
    {
        this.fileChooser.setTitle("Choose an view set for image reconstruction");
        this.fileChooser.setSelectedExtensionFilter( // Doesn't work; not sure why.
            new FileChooser.ExtensionFilter("View Set files", "*.vset"));
        if (reconstructionViewSetField.getText().isEmpty())
        {
            // Default for when the text field is empty.
            if (lastViewSet != null)
            {
                // There was a previously selected view set, use that one.
                this.fileChooser.setInitialDirectory(lastViewSet.getParentFile());
                this.fileChooser.setInitialFileName(lastViewSet.getName());
            }
        }
        else
        {
            // If the text field is not empty, use the current value as the starting directory in the file dialog.
            File currentValue = new File(reconstructionViewSetField.getText());
            this.fileChooser.setInitialDirectory(currentValue.getParentFile());
            this.fileChooser.setInitialFileName(currentValue.getName());
        }
        File file = this.fileChooser.showOpenDialog(stage.getOwner());
        if (file != null)
        {
            reconstructionViewSetField.setText(file.toString());
            lastViewSet = file;
        }
    }

    @FXML
    public void cancelButtonAction()
    {
        stage.close();
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(Consumer<IBRRequest<ContextType>> requestHandler)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            SpecularFitSettings settings = new SpecularFitSettings(
                    Integer.parseInt(widthTextField.getText()),
                    Integer.parseInt(heightTextField.getText()),
                    Integer.parseInt(basisCountTextField.getText()),
                    Integer.parseInt(mfdResolutionTextField.getText()),
                    new File(exportDirectoryField.getText()),
                    modelAccess.getSettingsModel());

            // Specular / general settings
            settings.setConvergenceTolerance(Double.parseDouble(convergenceToleranceTextField.getText()));
            settings.setSpecularSmoothness(Double.parseDouble(specularSmoothnessTextField.getText()));
            settings.setMetallicity(Double.parseDouble(metallicityTextField.getText()));

            // Normal estimation settings
            settings.setNormalRefinementEnabled(normalRefinementCheckBox.isSelected());
            settings.setMinNormalDamping(Double.parseDouble(minNormalDampingTextField.getText()));
            settings.setNormalSmoothingIterations(Integer.parseInt(normalSmoothingIterationsTextField.getText()));

            // Settings which shouldn't usually need to be changed
            settings.setSmithMaskingShadowingEnabled(smithCheckBox.isSelected());
            settings.setLevenbergMarquardtEnabled(levenbergMarquardtCheckBox.isSelected());;
            settings.setUnsuccessfulLMIterationsAllowed(Integer.parseInt(unsuccessfulLMIterationsTextField.getText()));

            if (reconstructionViewSetField.getText() != null && !reconstructionViewSetField.getText().equals(""))
            {
                // Reconstruction view set
                try
                {
                    settings.setReconstructionViewSet(ViewSet.loadFromVSETFile(
                        new File(reconstructionViewSetField.getText())));
                }
                catch (FileNotFoundException e)
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid view set");
                    alert.setHeaderText("Reconstruction view set is invalid.");
                    alert.setContentText("Please try another view set or leave the field blank to use the view set for the current model.");
                    e.printStackTrace();
                }
            }

            IBRRequest<ContextType> request = new SpecularFitRequest<>(settings);

            requestHandler.accept(request);
        });
    }
}
